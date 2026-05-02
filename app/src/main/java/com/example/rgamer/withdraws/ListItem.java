package com.example.rgamer.withdraws;

public class ListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    public int type;
    public String header;
    public CoinModel coin;

    public ListItem(String header) {
        this.type = TYPE_HEADER;
        this.header = header;
    }

    public ListItem(CoinModel coin) {
        this.type = TYPE_ITEM;
        this.coin = coin;
    }
}