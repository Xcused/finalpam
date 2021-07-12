package com.example.Listen.adapters;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.Listen.R;
import com.example.Listen.models.SongModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

//Mendeklarasikan Variable
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder>{

    List<SongModel> songs;
    List<SongModel> filteredSongs;
    List<SongModel> dbSongs;

    Context context;
    SongItemClickListener songItemClickListener;
    SongItemLongClickListener songItemLongClickListener;
    SongBtnClickListener songBtnClickListener;

    public SongAdapter(List<SongModel> songs, Context context) {
        this.songs = songs;
        this.dbSongs=songs;
        this.filteredSongs=new ArrayList<>();
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song,parent,false);
        return new ViewHolder(v);
    }

    //Menampilkan menu Play
    @Override
    public void onBindViewHolder(final SongAdapter.ViewHolder holder, final int position) {
        final SongModel song=songs.get(position);
        holder.tvSongName.setText(song.getTitle());
        holder.tvArtistName.setText(song.getArtistName());

        if(song.getAlbumArt() != "") {
            Picasso.with(context).load(song.getAlbumArt()).placeholder(R.drawable.default_song_art).into(holder.ivSongCoverArt);
        }
        holder.constraintLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(songItemClickListener != null ) {
                    songItemClickListener.onSongItemClick(view,song,position);
                }
            }
        });

        holder.constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                if(songItemLongClickListener != null) {
                    songItemLongClickListener.onSongItemLongClickListener(view,song,position);
                }
                return true;
            }

        });

        holder.songMenuBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(songBtnClickListener != null) {
                    songBtnClickListener.onSongBtnClickListener(holder.songMenuBtn,view,song,position);
                }
            }
        });

    }

    //Mendeklarasikan Variable
    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout constraintLayout;
        TextView tvSongName;
        TextView tvArtistName;
        ImageButton songMenuBtn;
        ImageView ivSongCoverArt;
        public ViewHolder(View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tv_song_name);
            tvArtistName=itemView.findViewById(R.id.tv_artist_name);
            songMenuBtn=itemView.findViewById(R.id.bt_song_menu);
            constraintLayout=itemView.findViewById(R.id.cl_song_item);
            ivSongCoverArt=itemView.findViewById(R.id.iv_song_coverart);
        }
    }

    public interface SongItemClickListener {
        void onSongItemClick(View v,SongModel song,int pos);
    }

    public interface SongItemLongClickListener {
        void onSongItemLongClickListener(View v, SongModel song, int pos);
    }

    public interface SongBtnClickListener {
        void onSongBtnClickListener(ImageButton btn, View v, SongModel song, int pos);
    }

    public void setOnSongItemClickListener(SongItemClickListener songItemClickListener) {
        this.songItemClickListener = songItemClickListener;
    }

    public void setOnSongItemLongClickListener(SongItemLongClickListener songItemLongClickListener) {
        this.songItemLongClickListener=songItemLongClickListener;
    }
    public void setOnSongBtnClickListener(SongBtnClickListener songBtnClickListener) {
        this.songBtnClickListener = songBtnClickListener;
    }

//Filter judul lagu
    public void filter(String filterKey) {
        filterKey = filterKey.toLowerCase();
        filteredSongs.clear();
        Log.d("Filter key is: ",filterKey);
        if (filterKey.length() == 0) {
            filteredSongs.clear();
        }
        else
        {
            for (SongModel song : dbSongs) {
                if (song.getTitle().toLowerCase().contains(filterKey) || song.getArtistName().toLowerCase().contains(filterKey)) {
                    filteredSongs.add(song);
                }
            }
            this.songs=filteredSongs;
        }
        notifyDataSetChanged();
    }
}
