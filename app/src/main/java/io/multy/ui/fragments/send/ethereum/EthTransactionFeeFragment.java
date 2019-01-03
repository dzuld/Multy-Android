/*
 * Copyright 2018 Idealnaya rabota LLC
 * Licensed under Multy.io license.
 * See LICENSE for details
 */

package io.multy.ui.fragments.send.ethereum;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.multy.R;
import io.multy.model.entities.Fee;
import io.multy.ui.activities.AssetSendActivity;
import io.multy.ui.activities.TokenSendActivity;
import io.multy.ui.adapters.MyFeeAdapter;
import io.multy.ui.fragments.BaseFragment;
import io.multy.util.Constants;
import io.multy.util.analytics.Analytics;
import io.multy.util.analytics.AnalyticsConstants;
import io.multy.viewmodels.AssetSendViewModel;

public class EthTransactionFeeFragment extends BaseFragment
        implements MyFeeAdapter.OnCustomFeeClickListener {

    public static EthTransactionFeeFragment newInstance() {
        return new EthTransactionFeeFragment();
    }

    @BindView(R.id.scrollview)
    ScrollView scrollView;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindString(R.string.donation_format_pattern)
    String formatPattern;
    @BindView(R.id.switcher)
    SwitchCompat switcher;
    @BindView(R.id.text_donation_allow)
    TextView textDonationAllow;
    @BindView(R.id.text_donation_summ)
    TextView textDonationsSum;
    @BindView(R.id.input_donation)
    EditText inputDonation;
    @BindView(R.id.text_fee_currency)
    TextView textFeeCurrency;
    @BindView(R.id.text_fee_original)
    TextView textFeeOriginal;
    @BindView(R.id.group_donation)
    Group groupDonationBtc;
    @BindView(R.id.group_donation_views)
    Group groupDonation;
    @BindView(R.id.button_next)
    View buttonNext;
    private AssetSendViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_fee, container, false);
        ButterKnife.bind(this, view);
        viewModel = ViewModelProviders.of(getActivity()).get(AssetSendViewModel.class);
        setBaseViewModel(viewModel);
        buttonNext.setEnabled(false);
        textFeeOriginal.setText(Constants.ETH);
        viewModel.speeds.observe(this, speeds -> {
            setAdapter();
            if (viewModel.getWallet().isMultisig()) {
                viewModel.requestMultisigEstimates(viewModel.getWallet().getActiveAddress().getAddress());
            } else {
                buttonNext.setEnabled(true);
            }
        });
        viewModel.estimation.observe(this, estimation -> buttonNext.setEnabled(estimation != null));
        viewModel.requestFeeRates(viewModel.getWallet().getCurrencyId(), viewModel.getWallet().getNetworkId(),
                viewModel.getWallet().isMultisig() ? null : viewModel.getReceiverAddress().getValue());
        Analytics.getInstance(getActivity()).logTransactionFeeLaunch(viewModel.getChainId());
        groupDonation.setVisibility(View.GONE);
        groupDonationBtc.setVisibility(View.GONE);
        return view;
    }

    private void setAdapter() {
        recyclerView.setAdapter(new MyFeeAdapter(viewModel.speeds.getValue().asList(), viewModel.getFee(),
                this, MyFeeAdapter.FeeType.ETH));
    }

    @Override
    public void onDestroyView() {
        hideKeyboard(getActivity());
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SET_GAS && data.getExtras() != null) {
            String gasPrice = data.getExtras().getString(Constants.GAS_PRICE, "");
            String gasLimit = data.getExtras().getString(Constants.GAS_LIMIT, "");
            if (!TextUtils.isEmpty(gasPrice) && !TextUtils.isEmpty(gasLimit)) {
//                feeAdapter.setCustomFee(gasPrice, gasLimit);
            }
            //todo insert selected rate into viewmodel
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    public void onClickCustom() {
//        logTransactionFee(5);
//        if (getActivity() != null) {
//            EthCustomSpeedFragment fragment = (EthCustomSpeedFragment) getActivity().getSupportFragmentManager()
//                    .findFragmentByTag(EthCustomSpeedFragment.TAG);
//            if (fragment == null) {
//                fragment = EthCustomSpeedFragment.getInstance();
//            }
//            fragment.setTargetFragment(this, Constants.REQUEST_CODE_SET_GAS);
//            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment)
//                    .addToBackStack(EthCustomSpeedFragment.TAG).commit();
//        }
//    }
//
//    @Override
//    public void onClickFee(int position) {
//        //todo insert selected rate into viewmodel
//        logTransactionFee(position);
//    }

    public void showCustomFeeDialog(long currentValue, long currentGasLimit) {
//        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());


//        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(),android.R.style.Theme_Black_NoTitleBar_Fullscreen);



        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(),android.R.style.Theme_Light_NoTitleBar_Fullscreen);


        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_custom_eth_fee, null);
        dialogBuilder.setView(dialogView);


        final TextInputEditText inputGasLimit = dialogView.findViewById(R.id.input_gas_limit);
        String gasLimit = String.valueOf(currentGasLimit);
        if (gasLimit.contains(".")) {
            gasLimit = gasLimit.split("\\.")[0];
        }
        inputGasLimit.setText(gasLimit);
        inputGasLimit.setSelection(inputGasLimit.getText().length());




        final TextInputEditText inputGasPrice = dialogView.findViewById(R.id.input_gas_price);

        String fee = "1";
        if (currentValue % Math.pow(10, 9)== 0){
            fee = String.valueOf(currentValue / Math.pow(10, 9));
            if (fee.contains(".")) {
                fee = fee.split("\\.")[0];
            }
        } else {
            fee = String.valueOf(currentValue / Math.pow(10, 9));
        }

//        String fee = currentValue == -1 ? "1" : String.valueOf(currentValue / Math.pow(10, 9));
//        if (fee.contains(".")) {
//            fee = fee.split("\\.")[0];
//        }
        inputGasPrice.setText(fee);
        inputGasPrice.setSelection(inputGasPrice.getText().length());

        final LifecycleObserver lifecycleObserver = new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            public void onResume() {
                inputGasPrice.postDelayed(() -> showKeyboard(getActivity(), inputGasPrice), 150);
            }
        };
        getLifecycle().addObserver(lifecycleObserver);
        dialogBuilder.setOnDismissListener(dialog -> getLifecycle().removeObserver(lifecycleObserver));

        dialogBuilder.setTitle("");

        final ImageButton backButton = dialogView.findViewById(R.id.but_back_eth_custom_fee);


        dialogBuilder.setPositiveButton(R.string.done, (dialog, whichButton) -> {
            long customGasLimit = 21000l;

            if (!inputGasLimit.getText().toString().isEmpty() && (long)(Long.parseLong(inputGasLimit.getText().toString())) >= 21000l){
                customGasLimit = (long) (Long.parseLong(inputGasLimit.getText().toString()));
            }

            String currentGas = inputGasPrice.getText().toString();
            if (!currentGas.isEmpty() && Double.parseDouble(currentGas)>=1){
                long gas =(long) (Double.parseDouble(currentGas) * Math.pow(10, 9));
                ((MyFeeAdapter) recyclerView.getAdapter()).setCustomFee(gas, customGasLimit);
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM_SET, viewModel.getChainId());

            }

//            if (!inputGasPrice.getText().toString().isEmpty() && Integer.parseInt(inputGasPrice.getText().toString()) >= 1) {
//                ((MyFeeAdapter) recyclerView.getAdapter()).setCustomFee((long) (Long.valueOf(inputGasPrice.getText().toString()) * Math.pow(10, 9)), customGasLimit);
//                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM_SET, viewModel.getChainId());
//            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
            Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM_CANCEL, viewModel.getChainId());
        });

        dialogBuilder.setOnDismissListener(dialog -> {
            hideKeyboard(getActivity());
        });

        AlertDialog dialogM = dialogBuilder.create();


        backButton.setOnClickListener(view -> {
            long customGasLimit = 21000l;

            if (!inputGasLimit.getText().toString().isEmpty() && (long)(Long.parseLong(inputGasLimit.getText().toString())) >= 21000l){
                customGasLimit = (long) (Long.parseLong(inputGasLimit.getText().toString()));
            }

            String currentGas = inputGasPrice.getText().toString();
            if (!currentGas.isEmpty() && Double.parseDouble(currentGas)>=1){
                long gas =(long) (Double.parseDouble(currentGas) * Math.pow(10, 9));
                ((MyFeeAdapter) recyclerView.getAdapter()).setCustomFee(gas, customGasLimit);
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM_SET, viewModel.getChainId());

            }
            dialogM.dismiss();
//
//            if (!inputGasPrice.getText().toString().isEmpty() && Integer.parseInt(inputGasPrice.getText().toString()) >= 1) {
//                ((MyFeeAdapter) recyclerView.getAdapter()).setCustomFee((long) (Long.valueOf(inputGasPrice.getText().toString()) * Math.pow(10, 9)), customGasLimit);
//                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM_SET, viewModel.getChainId());
//            }
        });





        dialogM.show();
//        dialogBuilder.create().show();


    }

    @Override
    public void onClickFee(Fee fee) {
        viewModel.setFee(fee);
    }

    @Override
    public void onClickCustomFee(long currentValue, long limit) {
        showCustomFeeDialog(currentValue, limit);
        logTransactionFee(5);
    }

    @Override
    public void logTransactionFee(int position) {
        switch (position) {
            case 0:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_VERY_FAST, viewModel.getChainId());
                break;
            case 1:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_FAST, viewModel.getChainId());
                break;
            case 2:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_MEDIUM, viewModel.getChainId());
                break;
            case 3:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_SLOW, viewModel.getChainId());
                break;
            case 4:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_VERY_SLOW, viewModel.getChainId());
                break;
            case 5:
                Analytics.getInstance(getActivity()).logTransactionFee(AnalyticsConstants.TRANSACTION_FEE_CUSTOM, viewModel.getChainId());
                break;
        }
    }

    @OnClick(R.id.button_next)
    void onClickNext() {
        Fee selectedFee = ((MyFeeAdapter) recyclerView.getAdapter()).getSelectedFee();

        if (selectedFee != null) {
            viewModel.setFee(selectedFee);
            if (getActivity() instanceof TokenSendActivity) {
                ((TokenSendActivity) getActivity()).setFragment(R.string.send_amount, R.id.container, TokenAmountChooserFragment.newInstance());
            } else {
                ((AssetSendActivity) getActivity()).setFragment(R.string.send_amount, R.id.container, EthAmountChooserFragment.newInstance());
            }

        } else {
            Toast.makeText(getActivity(), R.string.choose_transaction_speed, Toast.LENGTH_SHORT).show();
        }
    }

}
