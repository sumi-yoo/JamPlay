package com.sumi.jamplay.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sumi.jamplay.R
import com.sumi.jamplay.data.db.AppDatabase
import com.sumi.jamplay.data.db.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val name = context.getString(R.string.favorites_playlist_name)
                    val id = name.hashCode().toLong()
                    // DB가 처음 생성될 때 딱 한 번 실행됨
                    db.execSQL("INSERT INTO playlists (id, name) VALUES (?, ?)", arrayOf(id, name))
                }
            })
            .build()
    }

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
}
