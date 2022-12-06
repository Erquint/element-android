/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.crypto.verification.self

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetSelfWaitItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationWaitingItem
import im.vector.app.features.crypto.verification.user.BaseEpoxyVerificationController
import im.vector.app.features.crypto.verification.user.VerificationTransactionData
import im.vector.app.features.crypto.verification.user.bottomDone
import im.vector.app.features.crypto.verification.user.gotIt
import im.vector.app.features.crypto.verification.user.renderAcceptDeclineRequest
import im.vector.app.features.crypto.verification.user.renderCancel
import im.vector.app.features.crypto.verification.user.renderQrTransaction
import im.vector.app.features.crypto.verification.user.renderSasTransaction
import im.vector.app.features.crypto.verification.user.renderStartTransactionOptions
import im.vector.app.features.crypto.verification.user.verifiedSuccessTile
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import javax.inject.Inject

class SelfVerificationController @Inject constructor(
        stringProvider: StringProvider,
        colorProvider: ColorProvider,
        eventHtmlRenderer: EventHtmlRenderer,
) : BaseEpoxyVerificationController(stringProvider, colorProvider, eventHtmlRenderer) {

    interface InteractionListener : BaseEpoxyVerificationController.InteractionListener {
        fun onClickRecoverFromPassphrase()
        fun onClickSkip()
        fun onClickResetSecurity()
        fun onDoneFrom4S()
        fun keysNotIn4S()
    }

    var state: SelfVerificationViewState? = null

    fun update(state: SelfVerificationViewState) {
//        Timber.w("VALR controller updated $state")
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val state = this.state ?: return
        when (state.pendingRequest) {
            Uninitialized -> {
                renderBaseNoActiveRequest(state)
            }
            else -> {
                renderRequest(state)
            }
        }
    }

    private fun renderBaseNoActiveRequest(state: SelfVerificationViewState) {
        if (state.verifyingFrom4SAction !is Uninitialized) {
            render4SCheckState(state)
        } else {
            renderNoRequestStarted(state)
        }
    }

    private fun renderRequest(state: SelfVerificationViewState) {
        val host = this
        when (state.pendingRequest) {
            Uninitialized -> {
                // let's add option to start one
                val styledText = stringProvider.getString(R.string.verify_new_session_notice)

                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(styledText.toEpoxyCharSequence())
                }

                bottomSheetDividerItem {
                    id("sep")
                }
                bottomSheetVerificationActionItem {
                    id("start")
                    title(host.stringProvider.getString(R.string.start_verification))
                    titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    // subTitle(host.stringProvider.getString(R.string.verification_request_start_notice))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                    listener { host.listener?.onClickOnVerificationStart() }
                }
            }
            is Loading -> {
                bottomSheetSelfWaitItem {
                    id("waiting")
                }
//                bottomSheetVerificationWaitingItem {
//                    id("waiting_pr_loading")
//                    // title(host.stringProvider.getString(R.string.verification_request_waiting_for, state.otherUserMxItem.getBestName()))
//                }
            }
            is Success -> {
                val pendingRequest = state.pendingRequest.invoke()
                when (pendingRequest.state) {
                    EVerificationState.WaitingForReady -> {
                        bottomSheetSelfWaitItem {
                            id("waiting")
                        }
                    }
                    EVerificationState.Requested -> {
                        // add accept buttons?
                        renderAcceptDeclineRequest()
                        bottomSheetVerificationActionItem {
                            id("not me")
                            title(host.stringProvider.getString(R.string.verify_new_session_was_not_me))
                            titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                            iconRes(R.drawable.ic_arrow_right)
                            iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                            listener {
                                TODO()
//                                host.listener?.wasNotMe()
                            }
                        }
                    }
                    EVerificationState.Ready -> {
                        // add start options
                        renderStartTransactionOptions(pendingRequest, true)
                    }
                    EVerificationState.Started,
                    EVerificationState.WeStarted -> {
                        // nothing to do, in this case the active transaction is shown
                        renderActiveTransaction(state)
                    }
                    EVerificationState.WaitingForDone,
                    EVerificationState.Done -> {
                        verifiedSuccessTile()
                        bottomDone {
                            listener?.onDone(true)
                        }
                    }
                    EVerificationState.Cancelled -> {
                        renderCancel(pendingRequest.cancelConclusion ?: CancelCode.User)
                    }
                    EVerificationState.HandledByOtherSession -> {
                        // we should dismiss
                    }
                }
            }
            is Fail -> {
                // TODO
            }
        }
    }

    private fun renderNoRequestStarted(state: SelfVerificationViewState) {
        val host = this
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(host.stringProvider.getString(R.string.verification_verify_identity).toEpoxyCharSequence())
        }
        bottomSheetDividerItem {
            id("notice_div")
        }
        // Option to verify with another device
        if (state.hasAnyOtherSession) {
            bottomSheetVerificationActionItem {
                id("start")
                title(host.stringProvider.getString(R.string.verification_verify_with_another_device))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                //                    subTitle(host.stringProvider.getString(R.string.verification_request_start_notice))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { (host.listener as? InteractionListener)?.onClickOnVerificationStart() }
            }

            bottomSheetDividerItem {
                id("start_div")
            }
        }

        if (state.quadSContainsSecrets) {
            bottomSheetVerificationActionItem {
                id("passphrase")
                title(host.stringProvider.getString(R.string.verification_cannot_access_other_session))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                subTitle(host.stringProvider.getString(R.string.verification_use_passphrase))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { (host.listener as? InteractionListener)?.onClickRecoverFromPassphrase() }
            }

            bottomSheetDividerItem {
                id("start_div")
            }
        }

        // option to reset all
        bottomSheetVerificationActionItem {
            id("reset")
            title(host.stringProvider.getString(R.string.bad_passphrase_key_reset_all_action))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
            subTitle(host.stringProvider.getString(R.string.secure_backup_reset_all_no_other_devices))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
            listener { (host.listener as? InteractionListener)?.onClickResetSecurity() }
        }

        if (!state.isVerificationRequired) {
            bottomSheetDividerItem {
                id("reset_div")
            }

            bottomSheetVerificationActionItem {
                id("skip")
                title(host.stringProvider.getString(R.string.action_skip))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                listener { (host.listener as? InteractionListener)?.onClickSkip() }
            }
        }
    }

    private fun render4SCheckState(state: SelfVerificationViewState) {
        val host = this
        when (val action = state.verifyingFrom4SAction) {
            is Fail -> {
            }
            is Loading -> {
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(R.string.verification_request_waiting_for_recovery))
                }
            }
            is Success -> {
                val value = action.invoke()
                if (value) {
                    verifiedSuccessTile()
                    bottomDone { (host.listener as? InteractionListener)?.onDoneFrom4S() }
                } else {
                    bottomSheetVerificationNoticeItem {
                        id("notice_4s_failed'")
                        notice(
                                host.stringProvider.getString(
                                        R.string.error_failed_to_import_keys
                                )
                                        .toEpoxyCharSequence()
                        )
                    }
                    gotIt { (host.listener as? InteractionListener)?.keysNotIn4S() }
                }
            }
            else -> {
                // nop
            }
        }
    }

    private fun renderActiveTransaction(state: SelfVerificationViewState) {
        val transaction = state.startedTransaction
        val host = this
        when (transaction) {
            is Loading -> {
                // Loading => We are starting a transaction
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(R.string.please_wait))
                }
            }
            is Success -> {
                // Success => There is an active transaction
                renderTransaction(transaction = transaction.invoke())
            }
            is Fail -> {
                // todo
            }
            is Uninitialized -> {
            }
        }
    }

    private fun renderTransaction(transaction: VerificationTransactionData) {
        when (transaction) {
            is VerificationTransactionData.QrTransactionData -> {
                renderQrTransaction(transaction, null)
            }
            is VerificationTransactionData.SasTransactionData -> {
                renderSasTransaction(transaction)
            }
        }
    }
}
