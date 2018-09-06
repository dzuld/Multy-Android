/*
 * Copyright 2018 Idealnaya rabota LLC
 * Licensed under Multy.io license.
 * See LICENSE for details
 */

package io.multy.ui.fragments.asset;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import io.multy.R;
import io.multy.model.entities.TransactionHistory;
import io.multy.model.entities.TransactionOwner;
import io.multy.model.entities.wallet.CurrencyCode;
import io.multy.model.entities.wallet.Wallet;
import io.multy.storage.RealmManager;
import io.multy.ui.adapters.MultisigOwnersAdapter;
import io.multy.ui.fragments.BaseFragment;
import io.multy.ui.fragments.dialogs.AddressActionsDialogFragment;
import io.multy.ui.fragments.send.SendSummaryFragment;
import io.multy.util.Constants;
import io.multy.util.CryptoFormatUtils;
import io.multy.util.DateHelper;
import io.multy.util.NativeDataHelper;
import io.multy.viewmodels.WalletViewModel;
import io.reactivex.functions.Consumer;

import static io.multy.util.Constants.TX_MEMPOOL_INCOMING;
import static io.multy.util.Constants.TX_MEMPOOL_OUTCOMING;

/**
 * Created by anschutz1927@gmail.com on 16.01.18.
 */

public class MultisigTransactionInfoFragment extends BaseFragment {

    public static final String TAG = MultisigTransactionInfoFragment.class.getSimpleName();

    @BindView(R.id.linear_parent)
    LinearLayout parent;
    @BindView(R.id.scroll_view)
    NestedScrollView scrollView;
    @BindView(R.id.toolbar_name)
    TextView toolbarWalletName;
    @BindView(R.id.text_status)
    TextView textStatus;
    @BindView(R.id.image_operation)
    ImageView imageOperation;
    @BindView(R.id.text_value)
    TextView textValue;
    @BindView(R.id.text_coin)
    TextView textCoin;
    @BindView(R.id.text_amount)
    TextView textAmount;
    @BindView(R.id.text_money)
    TextView textMoney;
    @BindView(R.id.text_comment)
    TextView textComment;
    @BindView(R.id.text_addresses_from)
    TextView textAdressesFrom;
    @BindView(R.id.arrow)
    ImageView imageArrow;
    @BindView(R.id.logo)
    ImageView imageCoinLogo;
    @BindView(R.id.text_addresses_to)
    TextView textAddressesTo;
    @BindView(R.id.text_confirmations)
    TextView textConfirmations;
    @BindView(R.id.button_view)
    TextView buttonView;
    @BindView(R.id.text_counter)
    TextView textCounter;
    @BindView(R.id.recycler_owners)
    RecyclerView recyclerOwners;
    @BindView(R.id.progress)
    View progress;
    @BindView(R.id.image_slider_accept)
    View sliderAccept;
    @BindView(R.id.image_slider_decline)
    View sliderDecline;
    @BindView(R.id.button_confirm)
    TextView buttonConfirm;
    @BindView(R.id.group_data_views)
    Group groupDataViews;
    @BindView(R.id.group_view)
    Group groupViewButton;
    @BindColor(R.color.green_light)
    int colorGreen;
    @BindColor(R.color.blue_sky)
    int colorBlue;

    private WalletViewModel viewModel;
    private int walletIndex;
    private int currencyId;
    private int networkId;
    private int iconResourceId;
    private int confirmationsNeeded;
    private boolean isConfirming = false;
    private ValueAnimator animator;
    private MultisigOwnersAdapter adapter;
    private TransactionHistory transaction;
    private String walletAddress;

    public static MultisigTransactionInfoFragment newInstance(int transactionPosition) {
        MultisigTransactionInfoFragment fragment = new MultisigTransactionInfoFragment();
        Bundle args = new Bundle();
        args.putInt(TransactionInfoFragment.SELECTED_POSITION, transactionPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public MultisigTransactionInfoFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(getActivity()).get(WalletViewModel.class);
        setBaseViewModel(viewModel);
        adapter = new MultisigOwnersAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = getLayoutInflater().inflate(R.layout.fragment_multisig_transaction_info, container, false);
        ButterKnife.bind(this, v);
        recyclerOwners.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerOwners.setAdapter(adapter);
        viewModel.wallet.observe(this, this::onWallet);
        initAnimationSliders();
        //todo remove test
//        if (getArguments() != null && getArguments().getInt(TransactionInfoFragment.SELECTED_POSITION) == -1) {
//            setVisibilityConfirmButtons(View.VISIBLE);
//            animator.start();
//        }
        return v;
    }

    private void onWallet(Wallet wallet) {
        if (wallet != null) {
            walletIndex = wallet.getIndex();
            currencyId = wallet.getCurrencyId();
            networkId = wallet.getNetworkId();
            iconResourceId = wallet.getIconResourceId();
            walletAddress = wallet.getActiveAddress().getAddress();
            toolbarWalletName.setText(wallet.getWalletName());
            confirmationsNeeded = wallet.getMultisigWallet().getConfirmations();
            textCoin.setText(NativeDataHelper.Blockchain.valueOf(wallet.getCurrencyId()).name());
            textMoney.setText(CurrencyCode.USD.name());
            imageCoinLogo.setImageResource(iconResourceId);
            viewModel.transactions.observe(this, this::onHistory);
        }
    }

    private void onHistory(ArrayList<TransactionHistory> transactionHistories) {
        if (transactionHistories != null && getArguments() != null) {
            int position = getArguments().getInt(TransactionInfoFragment.SELECTED_POSITION);
            setData(transactionHistories.get(position));
        }
    }

    private void setData(TransactionHistory transactionHistory) {
        this.transaction = transactionHistory;
        boolean isIncoming = transactionHistory.getTxStatus() == Constants.TX_MEMPOOL_INCOMING;
        textValue.setText(isIncoming ? "+ " : "- ");
        textAmount.setText(isIncoming ? "+ " : "- ");
        parent.setBackgroundColor(isIncoming ? colorGreen : colorBlue);
        textStatus.setText(transactionHistory.getMultisigInfo().isConfirmed() ?
                getString(R.string.waiting_confirmations) : getTransactionDate(transactionHistory));
        if (transactionHistory.getMultisigInfo().isConfirmed()) {
            imageOperation.setImageResource(isIncoming ? R.drawable.ic_receive_big_new : R.drawable.ic_send_big);
            textConfirmations.setText(String.format(Locale.ENGLISH, "%d %s", transactionHistory.getConfirmations(),
                    getString(transactionHistory.getConfirmations() > 1 ?  R.string.confirmations : R.string.confirmation)));
            if (!transactionHistory.isInternal()) {
                groupViewButton.setVisibility(View.VISIBLE);
            }
            setVisibilityConfirmButtons(View.GONE);
        } else {
            imageOperation.setImageResource(isIncoming ? R.drawable.ic_receive_big_icon_waiting : R.drawable.ic_send_big_icon_waiting);
            enableAnimationSliders();
            if (!isOwnerVoted(transactionHistory.getMultisigInfo().getOwners())) {
                setVisibilityConfirmButtons(View.VISIBLE);
            }
        }
        textValue.append(getCurrencyAmount(transactionHistory));
        textAmount.append(getFiatAmount(transactionHistory, getPreferredExchangeRate(transactionHistory.getStockExchangeRates())));
        textAdressesFrom.setText(transactionHistory.getFrom());
        textAddressesTo.setText(transactionHistory.getTo());
        textCounter.setText(String.format(Locale.ENGLISH,
                "%d %s %d", getConfirmCount(transactionHistory.getMultisigInfo().getOwners()), getString(R.string.of), confirmationsNeeded));
        adapter.setOwners(transactionHistory.getMultisigInfo().getOwners());
    }

    private boolean isOwnerVoted(ArrayList<TransactionOwner> owners) {
        Wallet linkedWallet = RealmManager.getAssetsDao().getMultisigLinkedWallet(currencyId, networkId, walletIndex);
        for (TransactionOwner owner : owners) {
            if (owner.getAddress().equals(linkedWallet.getActiveAddress().getAddress())) {
                return owner.getConfirmationStatus() == Constants.MULTISIG_OWNER_STATUS_CONFIRMED ||
                        owner.getConfirmationStatus() == Constants.MULTISIG_OWNER_STATUS_DECLINED;
            }
        }
        return false;
    }

    private String getTransactionDate(TransactionHistory transaction) {
        return DateHelper.DATE_FORMAT_TRANSACTION_INFO
                .format((transaction.getTxStatus() == TX_MEMPOOL_INCOMING || transaction.getTxStatus() == TX_MEMPOOL_OUTCOMING ?
                        transaction.getMempoolTime() : transaction.getBlockTime()) * 1000);
    }

    private int getConfirmCount(ArrayList<TransactionOwner> owners) {
        int result = 0;
        for (TransactionOwner owner : owners) {
            if (owner.getConfirmationStatus() == Constants.MULTISIG_OWNER_STATUS_CONFIRMED) {
                result++;
            }
        }
        return result;
    }

    private String getCurrencyAmount(TransactionHistory transactionHistory) {
        return CryptoFormatUtils.FORMAT_ETH.format(CryptoFormatUtils.weiToEth(transactionHistory.getTxOutAmount()));
    }

    private String getFiatAmount(TransactionHistory transactionHistory, double exchangeRate) {
        return CryptoFormatUtils.ethToUsd(CryptoFormatUtils.weiToEth(transactionHistory.getTxOutAmount()), exchangeRate);
    }

    private double getPreferredExchangeRate(ArrayList<TransactionHistory.StockExchangeRate> stockExchangeRates) {
        if (stockExchangeRates != null && stockExchangeRates.size() > 0) {
            for (TransactionHistory.StockExchangeRate rate : stockExchangeRates) {
                if (rate.getExchanges().getEthUsd() > 0) {
                    return rate.getExchanges().getEthUsd();
                }
            }
        }
        return 0.0;
    }

    private void setVisibilityConfirmButtons(int visibility) {
        sliderAccept.setVisibility(visibility);
        sliderDecline.setVisibility(visibility);
        buttonConfirm.setVisibility(visibility);
    }

    private void initAnimationSliders() {
        animator = ValueAnimator.ofFloat(-0.8f, 0.8f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            float absValue = Math.abs((float) animation.getAnimatedValue());
            sliderAccept.setAlpha(0.2f + absValue);
            sliderDecline.setAlpha(1f - absValue);
            if (absValue > 0.4f && absValue < 0.415f) {
                buttonConfirm.setText(R.string.slide_to_confirm);
            } else if (absValue > 0.385f && absValue < 0.4f) {
                buttonConfirm.setText(R.string.slide_to_decline);
            }
        });
    }

    private void enableAnimationSliders() {
        if (animator != null && !animator.isRunning()) {
            animator.setStartDelay(2000);
            animator.start();
        }
    }

    private void disableAnimationSliders() {
        if (animator != null) {
            animator.cancel();
        }
        sliderAccept.setAlpha(0.2f);
        sliderDecline.setAlpha(0.2f);
    }

    private void hideConfirmButtonAnimation() {
        int buttonHeight = buttonConfirm.getHeight();
        ValueAnimator hideAnimator = ValueAnimator.ofInt(0, buttonHeight);
        hideAnimator.setInterpolator(new DecelerateInterpolator());
        hideAnimator.setDuration(1000);
        hideAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            buttonConfirm.setTranslationY(value);
            sliderAccept.setTranslationY(value);
            sliderDecline.setTranslationY(value);
        });
        hideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibilityConfirmButtons(View.GONE);
            }
        });
        hideAnimator.start();
    }

    private void readyViewsToSlide(View firstSlide, View secondSlide, int confirmText) {
        disableAnimationSliders();
        firstSlide.setAlpha(1f);
        secondSlide.setAlpha(0.2f);
        buttonConfirm.setText(confirmText);
    }

    private void onSliderMove(View view, float rawX, boolean isNegativeVector, Runnable onSliderFinish) {
        scrollView.requestDisallowInterceptTouchEvent(true);
        float x = isNegativeVector ? (buttonConfirm.getWidth() - (rawX + (view.getWidth() / 2))) : rawX - (view.getWidth() / 2);
        if (x > 0) {
            view.setTranslationX(isNegativeVector ? -x : x);
            float limit = isNegativeVector ? view.getX() : (buttonConfirm.getWidth() - view.getX() - view.getWidth());
            if (limit < 0 && !isConfirming) {
                isConfirming = true;
                view.clearFocus();
                hideConfirmButtonAnimation();
                onSliderFinish.run();
            }
        }
    }

    private void cancelSlide(View view) {
        view.performClick();
        enableAnimationSliders();
        view.setTranslationX(0f);
    }

    private void onAcceptTransaction() {
        Consumer<Throwable> onError = throwable -> {
            viewModel.errorMessage.setValue(throwable.getLocalizedMessage());
            throwable.printStackTrace();
        };
        viewModel.requestEstimation(walletAddress, estimationConfirm -> {
            viewModel.requestFeeRates(currencyId, networkId, mediumGasPrice -> {
                Wallet linkedWallet = RealmManager.getAssetsDao().getMultisigLinkedWallet(currencyId, networkId, walletIndex);
                byte[] tx = NativeDataHelper.confirmTransactionMultisigETH(RealmManager.getSettingsDao().getSeed().getSeed(),
                        walletIndex, 0, currencyId, networkId, linkedWallet.getActiveAddress().getAmountString(),
                        walletAddress, transaction.getMultisigInfo().getIndex(),
                        estimationConfirm, mediumGasPrice, linkedWallet.getEthWallet().getNonce());
                Log.e(getClass().getSimpleName(), SendSummaryFragment.byteArrayToHex(tx));
            }, onError);
        }, onError);
    }

    private void onDeclineTransaction() {
        //todo make decline
        Log.i("OnSwipe", "declined!");
    }

    @OnTouch(R.id.image_slider_accept)
    boolean onTouchAccept(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                readyViewsToSlide(sliderAccept, sliderDecline, R.string.slide_to_confirm);
                return true;
            case MotionEvent.ACTION_MOVE:
                onSliderMove(view, event.getRawX(), false, this::onAcceptTransaction);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelSlide(view);
                return true;
        }
        return false;
    }

    @OnTouch(R.id.image_slider_decline)
    boolean onTouchDecline(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                readyViewsToSlide(sliderDecline, sliderAccept, R.string.slide_to_decline);
                return true;
            case MotionEvent.ACTION_MOVE:
                onSliderMove(view, event.getRawX(), true, this::onDeclineTransaction);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelSlide(view);
                return true;
        }
        return false;
    }

    @OnClick({R.id.text_addresses_from, R.id.text_addresses_to})
    void onClickAddress(View view) {
        if (view instanceof TextView) {
            AddressActionsDialogFragment.getInstance(((TextView) view).getText().toString(),
                    currencyId, networkId, iconResourceId, true, () ->
//                            todo notify observers
                            viewModel.transactions.setValue(viewModel.transactions.getValue()))
                    .show(getChildFragmentManager(), AddressActionsDialogFragment.TAG);
        }
    }

    @OnClick(R.id.back)
    void onClickBack() {
        getActivity().onBackPressed();
    }
}