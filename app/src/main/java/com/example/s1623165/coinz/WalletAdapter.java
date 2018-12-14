package com.example.s1623165.coinz;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// The Wallet Adapter and Wallet View holders are custom classes to be used by the recycler view
// in the wallet fragment. They display the correct instances of coinitems and set the onclick listeners.
// The functionality is implemented in walletfragment
public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

    private ArrayList<CoinItem> wallet;
    private OnItemClickListener mListener;
    private OnSendClickListener sendListener;

    public interface OnSendClickListener {
        void onSendClick(int position);
    }

    public interface OnItemClickListener {
        void onBankClick(int position);
    }

    public void setOnSendClickListener(OnSendClickListener listener) { sendListener = listener;}

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class WalletViewHolder extends RecyclerView.ViewHolder {

        public ImageView coinImage;
        public TextView title;
        public TextView description;
        public ImageView bankImage;
        public ImageView sendImage;

        public WalletViewHolder(@NonNull View itemView, final OnItemClickListener listener, final OnSendClickListener sendListener) {
            super(itemView);
            coinImage = itemView.findViewById(R.id.coinImage);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
            bankImage = itemView.findViewById(R.id.bankImage);
            sendImage = itemView.findViewById(R.id.sendImage);

            bankImage.setOnClickListener(v -> {
                if(listener != null) {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION) {
                        listener.onBankClick(position);
                    }
                }
            });
            sendImage.setOnClickListener(v -> {
                if(sendListener != null) {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION) {
                        sendListener.onSendClick(position);
                    }
                }
            });
        }
    }

    public WalletAdapter(ArrayList<CoinItem> coinList) {
        wallet = coinList;
    }

    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.coin_item, viewGroup, false);
        WalletViewHolder wvh = new WalletViewHolder(v, mListener, sendListener);
        return wvh;
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder walletViewHolder, int i) {
        CoinItem currentCoin = wallet.get(i);

        walletViewHolder.coinImage.setImageResource(currentCoin.getCoinImageResource());
        walletViewHolder.title.setText(currentCoin.getTitle());
        walletViewHolder.description.setText(currentCoin.getDescription());
    }

    @Override
    public int getItemCount() {
        return wallet.size();
    }
}
