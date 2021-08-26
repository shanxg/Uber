package com.lucasrivaldo.cloneuber.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucasrivaldo.cloneuber.R;
import com.lucasrivaldo.cloneuber.helper.UberHelper;
import com.lucasrivaldo.cloneuber.model.Trip;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterTrips extends RecyclerView.Adapter<AdapterTrips.TripsViewHolder> {

    private List<Trip> tripList;
    private Context context;

    public AdapterTrips(List<Trip> tripList, Context context) {
        this.tripList = tripList;
        this.context = context;
    }

    @NonNull
    @Override
    public TripsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemList = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_trip_requests, parent, false);

        return new TripsViewHolder(itemList);
    }

    @Override
    public void onBindViewHolder(@NonNull TripsViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        holder.civ_uberAdapterTrips.setImageResource
                (UberHelper.returnImageResourceId(trip.getTripType()));

        holder.textReqUberType.setText
                (UberHelper.returnTypeName(trip.getTripType(), context.getResources()));

        holder.searchMyLocation.setQueryHint(trip.getStartLoc().getAddressLines());
        holder.searchMyLocation.setEnabled(false);
        holder.searchMyLocation.setFocusable(false);

        holder.searchDestinyLocation.setQueryHint(trip.getDestination().getAddressLines());
        holder.searchDestinyLocation.setEnabled(false);
        holder.searchDestinyLocation.setFocusable(false);

        holder.textReqDistance.setText(trip.getDistanceText());
        holder.textReqDuration.setText(trip.getDurationText());
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    class TripsViewHolder extends RecyclerView.ViewHolder{

        private SearchView searchMyLocation, searchDestinyLocation;
        private TextView textReqUberType, textReqDuration, textReqDistance;
        private CircleImageView civ_uberAdapterTrips;


        TripsViewHolder(@NonNull View itemView) {
            super(itemView);

            searchMyLocation = itemView.findViewById(R.id.searchMyLocation);
            searchDestinyLocation = itemView.findViewById(R.id.searchDestinyLocation);

            itemView.findViewById(R.id.btnMyLoc).setVisibility(View.GONE);
            itemView.findViewById(R.id.btnAddNewLoc).setVisibility(View.GONE);

            civ_uberAdapterTrips = itemView.findViewById(R.id.civ_uberAdapterTrips);

            textReqUberType = itemView.findViewById(R.id.textReqUberType);
            textReqDuration = itemView.findViewById(R.id.textReqDuration);
            textReqDistance = itemView.findViewById(R.id.textReqDistance);
        }
    }


}
