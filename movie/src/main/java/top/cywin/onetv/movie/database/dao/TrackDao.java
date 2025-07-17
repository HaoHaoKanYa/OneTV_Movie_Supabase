package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.Track;

import java.util.List;

/**
 * 轨道DAO
 * 基于FongMi_TV的TrackDao完整移植
 */
@Dao
public abstract class TrackDao extends BaseDao<Track> {

    @Query("SELECT * FROM Track WHERE `key` = :key")
    public abstract List<Track> find(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Long insert(Track item);

    @Query("DELETE FROM Track WHERE `key` = :key")
    public abstract void delete(String key);
}
