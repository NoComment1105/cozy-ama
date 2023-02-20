/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.components.ComponentRegistry
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TopGuildChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import org.koin.core.component.inject
import org.quiltmc.community.ama.database.collections.AmaConfigCollection
import org.quiltmc.community.ama.database.entities.AmaConfig
import org.quiltmc.community.ama.database.entities.AmaEmbedConfig
import org.quiltmc.community.ama.enums.QuestionStatusFlag
import org.quiltmc.community.ama.questionComponents
import org.quiltmc.community.ama.questionEmbed

class AmaExtension : Extension() {
	override val name: String = "ama"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "ama"
			description = "The command for using AMA"

			ephemeralSubCommand(::AmaConfigArgs, ::AmaConfigModal) {
				name = "config"
				description = "Configure your AMA settings"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requirePermission(Permission.SendMessages)
				}

				var buttonMessage: Message?
				action { modal ->
					val embedConfig = AmaEmbedConfig(modal?.header?.value!!, modal.body.value, modal.image.value)
					buttonMessage = arguments.buttonChannel.asChannelOf<GuildMessageChannel>().createMessage {
						embed {
							title = embedConfig.title
							description = embedConfig.description
							image = embedConfig.imageUrl
						}
					}

					val buttonId = "ama-button.${buttonMessage!!.id}"

					buttonMessage!!.edit {
						val components = components {
							ephemeralButton {
								label = "Ask a question"
								style = ButtonStyle.Secondary

								id = buttonId
								disabled = true
								action { }
							}
						}
						components.removeAll()
					}

					AmaConfigCollection().setConfig(
						AmaConfig(
							guild!!.id,
							arguments.answerQueueChannel.id,
							arguments.liveChatChannel.id,
							arguments.buttonChannel.id,
							arguments.approvalQueue?.id,
							arguments.flaggedQuestionChannel?.id,
							embedConfig,
							buttonMessage!!.id,
							buttonId,
							false
						)
					)

					respond { content = "Set AMA config" }
				}
			}

			ephemeralSubCommand {
				name = "start"
				description = "Start the AMA for this server"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requirePermission(Permission.SendMessages)
				}

				action {
					val config = AmaConfigCollection().getConfig(guild!!.id)

					if (config == null) {
						respond {
							content = "There is no AMA config for this guild!"
						}
						return@action
					}

					if (config.enabled) {
						respond {
							content = "AMA is already started for this guild"
						}
						return@action
					}

					guild!!.getChannelOf<GuildMessageChannel>(config.buttonChannel).getMessage(config.buttonMessage)
						.edit {
							components?.removeFirst()
							val newComponents = components {
								ephemeralButton {
									label = "Ask a question"
									style = ButtonStyle.Primary

									id = config.buttonId
									action { }
								}
							}
							newComponents.removeAll()
						}

					AmaConfigCollection().modifyButton(guild!!.id, true)

					respond { content = "AMA Started!" }
				}
			}

			ephemeralSubCommand {
				name = "stop"
				description = "Stop the AMA for this server"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requirePermission(Permission.SendMessages)
				}

				action {
					val config = AmaConfigCollection().getConfig(guild!!.id)

					if (config == null) {
						respond {
							content = "There is no AMA config for this guild!"
						}
						return@action
					}

					if (!config.enabled) {
						respond {
							content = "AMA is already stopped for this guild"
						}
						return@action
					}

					guild!!.getChannelOf<GuildMessageChannel>(config.buttonChannel).getMessage(config.buttonMessage)
						.edit {
							components?.removeFirst()
							val newComponents = components {
								ephemeralButton {
									label = "Ask a question"
									style = ButtonStyle.Primary

									id = config.buttonId
									disabled = true
									action { }
								}
							}
							newComponents.removeAll()
						}

					AmaConfigCollection().modifyButton(guild!!.id, false)

					respond { content = "AMA Stopped!" }
				}
			}
		}

		event<ButtonInteractionCreateEvent> {
			check {
				anyGuild()
				failIfNot { event.interaction.componentId.contains("ama-button.") }
			}

			action {
				// Start modal creation
				val modalObj = AskModal()
				val componentRegistry: ComponentRegistry by inject()
				var interactionResponse: EphemeralMessageInteractionResponseBehavior? = null

				componentRegistry.register(modalObj)

				event.interaction.modal(modalObj.title, modalObj.id) {
					modalObj.applyToBuilder(this, getLocale(), null)
				}

				modalObj.awaitCompletion { modalSubmitInteraction ->
					interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
				}
				// End modal creation

				val config = AmaConfigCollection().getConfig(guildFor(event)!!.id) ?: return@action

				val channelId = config.approvalQueueChannel ?: config.answerQueueChannel
				val channel = guildFor(event)?.getChannelOf<GuildMessageChannel>(channelId)

				val originalInteractionUser = event.interaction.user
				val embedMessage = channel?.createEmbed {
					questionEmbed(originalInteractionUser, modalObj.question.value, QuestionStatusFlag.NO_FLAG)
				}

				val answerQueueChannel = guildFor(event)?.getChannelOf<GuildMessageChannel>(config.answerQueueChannel)
				val liveChatChannel = guildFor(event)?.getChannelOf<GuildMessageChannel>(config.liveChatChannel)
				val flaggedQueueChannel = if (config.flaggedQuestionChannel != null) {
					guildFor(event)?.getChannelOf<GuildMessageChannel>(config.flaggedQuestionChannel)
				} else {
					null
				}

				embedMessage?.edit {
					components {
						questionComponents(
							embedMessage,
							originalInteractionUser,
							modalObj.question.value,
							answerQueueChannel,
							liveChatChannel,
							flaggedQueueChannel,
							config.flaggedQuestionChannel
						)
					}
				}

				interactionResponse?.createEphemeralFollowup { content = "Question sent!" }
			}
		}

		unsafeSlashCommand {
			name = "ask"
			description = "Ask a question for the current AMA"

			initialResponse = InitialSlashCommandResponse.None

			check { anyGuild() }

			action {
				if (AmaConfigCollection().isButtonEnabled(guild!!.id) == false) {
					ackEphemeral()
					respondEphemeral { content = "The AMA is not running!" }
					return@action
				}
				// Start modal creation
				val modalObj = AskModal()

				this@unsafeSlashCommand.componentRegistry.register(modalObj)

				event.interaction.modal(modalObj.title, modalObj.id) {
					modalObj.applyToBuilder(this, getLocale(), null)
				}

				modalObj.awaitCompletion { modalSubmitInteraction ->
					interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
				}

				// End modal creation
				val config = AmaConfigCollection().getConfig(guild!!.id) ?: return@action

				val channelId = config.approvalQueueChannel ?: config.answerQueueChannel
				val channel = guild?.getChannelOf<GuildMessageChannel>(channelId)

				val originalInteractionUser = event.interaction.user
				val embedMessage = channel?.createEmbed {
					questionEmbed(originalInteractionUser, modalObj.question.value, QuestionStatusFlag.NO_FLAG)
				}

				val answerQueueChannel = guild?.getChannelOf<GuildMessageChannel>(config.answerQueueChannel)
				val liveChatChannel = guild?.getChannelOf<GuildMessageChannel>(config.liveChatChannel)
				val flaggedQueueChannel = if (config.flaggedQuestionChannel != null) {
					guildFor(event)?.getChannelOf<GuildMessageChannel>(config.flaggedQuestionChannel)
				} else {
					null
				}

				embedMessage?.edit {
					components {
						questionComponents(
							embedMessage,
							originalInteractionUser,
							modalObj.question.value,
							answerQueueChannel,
							liveChatChannel,
							flaggedQueueChannel,
							config.flaggedQuestionChannel
						)
					}
				}

				interactionResponse?.createEphemeralFollowup { content = "Question Sent" }
			}
		}
	}

	suspend inline fun Channel?.checkPermission(permissions: Permissions): Boolean? {
		this ?: return null
		val topGuildChannel = this.asChannelOfOrNull<TopGuildChannel>() ?: return null
		return topGuildChannel.getEffectivePermissions(kord.selfId).contains(permissions)
	}

	inner class AmaConfigArgs : Arguments() {
		val answerQueueChannel by channel {
			name = "answer-queue-channel"
			description = "The channel for asked questions to queue up in before response"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
			validate {
				val checkResult = value.checkPermission(Permissions(Permission.ViewChannel, Permission.SendMessages))
				failIf(checkResult == false, "The bot cannot see this message")
				failIf(checkResult == null, "Cannot find this channel!")
			}
		}

		val liveChatChannel by channel {
			name = "live-chat-channel"
			description = "The channel questions will be sent to when answered by staff"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
			validate {
				val checkResult = value.checkPermission(Permissions(Permission.ViewChannel, Permission.SendMessages))
				failIf(checkResult == false, "The bot cannot see this message")
				failIf(checkResult == null, "Cannot find this channel!")
			}
		}

		val buttonChannel by channel {
			name = "button-channel"
			description = "The channel the button for asking questions with is in"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
			validate {
				val checkResult = value.checkPermission(Permissions(Permission.ViewChannel, Permission.SendMessages))
				failIf(checkResult == false, "The bot cannot see this message")
				failIf(checkResult == null, "Cannot find this channel!")
			}
		}

		val approvalQueue by optionalChannel {
			name = "approval-queue"
			description = "The channel for questions to get sent to, for approval"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
			validate {
				val checkResult = value.checkPermission(Permissions(Permission.ViewChannel, Permission.SendMessages))
				failIf(checkResult == false, "The bot cannot see this message")
			}
		}

		val flaggedQuestionChannel by optionalChannel {
			name = "flagged-question-channel"
			description = "The channel for questions flagged by moderators to be sent too"
			requiredChannelTypes = mutableSetOf(ChannelType.GuildText)
			validate {
				val checkResult = value.checkPermission(Permissions(Permission.ViewChannel, Permission.SendMessages))
				failIf(checkResult == false, "The bot cannot see this message")
			}
		}
	}

	inner class AmaConfigModal : ModalForm() {
		override var title: String = "Configure your AMA Session"

		val header = lineText {
			label = "AMA Embed Title"
			placeholder = "Ask me anything session!"
			maxLength = 200
			required = true
		}

		val body = paragraphText {
			label = "AMA Embed body"
			placeholder = "Ask me any question you can think of!"
			maxLength = 1_800
			required = false
		}

		val image = lineText {
			label = "AMA Embed image"
			placeholder = "https://i.imgur.com/yRgfdVJ.png"
			required = false
		}
	}

	inner class AskModal : ModalForm() {
		override var title: String = "Ask a question"

		val question = paragraphText {
			label = "Question"
			placeholder = "What are you doing today?"
			required = true
		}
	}
}
