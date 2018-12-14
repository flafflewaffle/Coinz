package com.example.s1623165.coinz;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// The Powerup Adapter and Powerup View holders are custom classes to be used by the recycler view
// in the wallet fragment. They display the correct instances of poweritems and set the onclick listeners.
// The functionality is implemented in powerup fragment

public class PowerupAdapter extends RecyclerView.Adapter<PowerupAdapter.PowerupViewHolder> {
    private ArrayList<PowerItem> powerups;
    private PowerupAdapter.OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onActivateClick(int position);
    }

    public void setOnItemClickListener(PowerupAdapter.OnItemClickListener listener) {
        mListener = listener;
    }

    public static class PowerupViewHolder extends RecyclerView.ViewHolder {

        public ImageView powerImage;
        public TextView title;
        public ImageView activateImage;


        public PowerupViewHolder(@NonNull View itemView, final PowerupAdapter.OnItemClickListener listener) {
            super(itemView);
            powerImage = itemView.findViewById(R.id.powerupImage);
            title = itemView.findViewById(R.id.textTitle);
            activateImage = itemView.findViewById(R.id.activateImage);

            activateImage.setOnClickListener(v -> {
                if(listener != null) {
                    int position = getAdapterPosition();
                    if(position != RecyclerView.NO_POSITION) {
                        listener.onActivateClick(position);
                    }
                }
            });
        }
    }

    public PowerupAdapter(ArrayList<PowerItem> powerupList) {
        powerups = powerupList;
    }

    @NonNull
    @Override
    public PowerupAdapter.PowerupViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.powerup_item, viewGroup, false);
        PowerupAdapter.PowerupViewHolder pvh = new PowerupAdapter.PowerupViewHolder(v, mListener);
        return pvh;
    }

    @Override
    public void onBindViewHolder(@NonNull PowerupAdapter.PowerupViewHolder powerViewHolder, int i) {
        PowerItem powerup = powerups.get(i);
        powerViewHolder.powerImage.setImageResource(powerup.getPowerImageResource());
        powerViewHolder.title.setText(powerup.getTitle());
    }

    @Override
    public int getItemCount() {
        return powerups.size();
    }
}
