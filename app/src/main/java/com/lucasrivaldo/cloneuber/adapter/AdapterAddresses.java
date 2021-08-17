package com.lucasrivaldo.cloneuber.adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.model.Trip;
import com.lucasrivaldo.cloneuber.model.UberAddress;

import java.util.List;
import java.util.zip.Inflater;

public class AdapterAddresses extends RecyclerView.Adapter<AdapterAddresses.AddressesHolder> {

    private List<UberAddress> addressList;

    public AdapterAddresses(List<UberAddress> addressList) {
        this.addressList = addressList;
    }

    class AddressesHolder extends RecyclerView.ViewHolder {

        TextView textAddress;
        ImageView adapterIcon;

        public AddressesHolder(@NonNull View itemView) {
            super(itemView);

            textAddress = itemView.findViewById(R.id.textAddress);
            adapterIcon = itemView.findViewById(R.id.adapterIcon);
        }
    }

    @NonNull
    @Override
    public AddressesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemList = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_address, parent, false);

        return new AddressesHolder(itemList);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressesHolder holder, int position) {

        UberAddress address = addressList.get(position);


        if (address.getAddressLines().equals(UberAddress.CURRENT_LOC))
            holder.adapterIcon.setImageResource(R.drawable.ic_my_location_black_24dp);

        else if (addressList.size() == 2 && address.getAddressLines().equals(UberAddress.NO_LOC_FOUND)) {
            holder.adapterIcon.setVisibility(View.GONE);
            holder.textAddress.setGravity(Gravity.CENTER);
            holder.textAddress.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        }

        holder.textAddress.setText(address.getAddressLines());

    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

}
