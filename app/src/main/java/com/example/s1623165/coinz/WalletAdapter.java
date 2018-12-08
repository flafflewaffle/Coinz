package com.example.s1623165.coinz;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private ArrayList<CoinItem> wallet;

    public static class WalletViewHolder extends RecyclerView.ViewHolder {

        public ImageView image;
        public TextView title;
        public TextView description;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.coinImage);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
        }
    }

    public WalletAdapter(ArrayList<CoinItem> coinList) {
        wallet = coinList;
    }

    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.coin_item, viewGroup, false);
        WalletViewHolder wvh = new WalletViewHolder(v);
        return wvh;
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder walletViewHolder, int i) {
        CoinItem currentCoin = wallet.get(i);

        walletViewHolder.image.setImageResource(currentCoin.getCoinImageResource());
        walletViewHolder.title.setText(currentCoin.getTitle());
        walletViewHolder.description.setText(currentCoin.getDescription());
    }

    @Override
    public int getItemCount() {
        return wallet.size();
    }
}
