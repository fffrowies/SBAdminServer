package com.fffrowies.sbadminserver.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.TextView;

import com.fffrowies.sbadminserver.Interface.ItemClickListener;
import com.fffrowies.sbadminserver.R;

public class OrderViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener {

    public TextView txvOrderId, txvOrderStatus, txvOrderPhone, txvOrderAddress;

    private ItemClickListener itemClickListener;

    public OrderViewHolder(@NonNull View itemView) {
        super(itemView);

        txvOrderId = (TextView) itemView.findViewById(R.id.order_id);
        txvOrderStatus = (TextView) itemView.findViewById(R.id.order_status);
        txvOrderPhone = (TextView) itemView.findViewById(R.id.order_phone);
        txvOrderAddress = (TextView) itemView.findViewById(R.id.order_address);

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        itemView.setOnCreateContextMenuListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onClick(v, getAdapterPosition(), false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select The Action");

        menu.add(0, 0, getAdapterPosition(), "Update");
        menu.add(0, 1, getAdapterPosition(), "Delete");
    }

    @Override
    public boolean onLongClick(View v) {
        itemClickListener.onClick(v, getAdapterPosition(), true);
        return true;
    }
}
