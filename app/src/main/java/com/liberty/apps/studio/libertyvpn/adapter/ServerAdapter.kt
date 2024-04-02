package com.liberty.apps.studio.libertyvpn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.liberty.apps.studio.libertyvpn.R
import com.liberty.apps.studio.libertyvpn.model.Server
import com.liberty.apps.studio.libertyvpn.utils.OvpnUtils
import java.util.Locale

/**
 * Copyright (C) 2015  Jhon Kenneth Carino
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 *
 * Created by Jhon Kenneth Carino on 10/25/15.
 */
class ServerAdapter(servers: List<Server>?, callback: ServerClickCallback) :
    RecyclerView.Adapter<ServerAdapter.ViewHolder>() {
    /*
    * Set the server the data
    * */
    private val servers: MutableList<Server>? = ArrayList()
    private val callback: ServerClickCallback

    init {
        this.servers!!.clear()
        this.servers.addAll(servers!!)
        this.callback = callback
    }

    fun setServerList(serverList: List<Server>) {
        if (servers!!.isEmpty()) {
            servers.clear()
            servers.addAll(serverList)
            notifyItemRangeInserted(0, serverList.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return servers.size
                }

                override fun getNewListSize(): Int {
                    return serverList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = servers[oldItemPosition]
                    val server = serverList[newItemPosition]
                    return old.hostName == server.hostName
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val old = servers[oldItemPosition]
                    val server = serverList[newItemPosition]
                    return old.hostName == server.hostName && old.ipAddress == server.ipAddress && old.countryLong == server.countryLong
                }
            })
            servers.clear()
            servers.addAll(serverList)
            result.dispatchUpdatesTo(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_list_item, parent, false)
        return ViewHolder(view, callback)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(servers!![position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return servers?.size ?: 0
    }

    class ViewHolder(val rootView: View, val callback: ServerClickCallback) :
        RecyclerView.ViewHolder(
            rootView
        ) {
        val countryView: TextView
        val protocolView: TextView
        val ipAddressView: TextView
        val speedView: TextView
        val pingView: TextView

        init {
            countryView = rootView.findViewById(R.id.tv_country_name)
            protocolView = rootView.findViewById(R.id.tv_protocol)
            ipAddressView = rootView.findViewById(R.id.tv_ip_address)
            speedView = rootView.findViewById(R.id.tv_speed)
            pingView = rootView.findViewById(R.id.tv_ping)
        }

        fun bind(server: Server) {
            val context = rootView.context
            countryView.text = server.countryLong
            protocolView.text = server.protocol.uppercase(Locale.getDefault())
            ipAddressView.text = context.getString(
                R.string.format_ip_address,
                server.ipAddress, server.port
            )
            speedView.text = context.getString(
                R.string.format_speed,
                OvpnUtils.humanReadableCount(server.speed, true)
            )
            pingView.text = context.getString(R.string.format_ping, server.ping)
            rootView.setOnClickListener { callback.onItemClick(server) }
        }
    }

    interface ServerClickCallback {
        fun onItemClick(server: Server)
    }
}
