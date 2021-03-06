/*
 * Copyright 2018 Idealnaya rabota LLC
 * Licensed under Multy.io license.
 * See LICENSE for details
 */

package io.multy.api.socket;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class CurrenciesRate extends RealmObject {

    @PrimaryKey
    private int id = 0;
    @SerializedName("btc_usd")
    private double btcToUsd = 1;
    @SerializedName("btc_eur")
    private double btcToEur = 1;

    @SerializedName("eth_usd")
    private double ethToUsd = 1;
    @SerializedName("eth_eur")
    private double ethToEur = 1;

    @SerializedName("eos_usd")
    private double eosToUsd = 1;
    @SerializedName("eos_eur")
    private double eosToEur = 1;

    public double getEosToUsd() {
        return eosToUsd;
    }

    public void setEosToUsd(double eosToUsd) {
        this.eosToUsd = eosToUsd;
    }

    public double getEosToEur() {
        return eosToEur;
    }

    public void setEosToEur(double eosToEur) {
        this.eosToEur = eosToEur;
    }

    public double getBtcToUsd() {
        return btcToUsd;
    }

    public void setBtcToUsd(double btcToUsd) {
        this.btcToUsd = btcToUsd;
    }

    public double getBtcToEur() {
        return btcToEur;
    }

    public void setBtcToEur(double btcToEur) {
        this.btcToEur = btcToEur;
    }

    public double getEthToUsd() {
        return ethToUsd;
    }

    public void setEthToUsd(double ethToUsd) {
        this.ethToUsd = ethToUsd;
    }

    public double getEthToEur() {
        return ethToEur;
    }

    public void setEthToEur(double ethToEur) {
        this.ethToEur = ethToEur;
    }
}
