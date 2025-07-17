package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import top.cywin.onetv.movie.bean.Config;

import java.util.List;

/**
 * 配置DAO
 * 基于FongMi_TV的ConfigDao完整移植
 */
@Dao
public abstract class ConfigDao extends BaseDao<Config> {

    @Query("SELECT * FROM Config")
    public abstract List<Config> findAll();

    @Query("SELECT * FROM Config WHERE type = :type ORDER BY time DESC")
    public abstract List<Config> findByType(int type);

    @Query("SELECT * FROM Config WHERE type = :type ORDER BY time DESC")
    public abstract List<Config> findUrlByType(int type);

    @Query("SELECT * FROM Config WHERE id = :id")
    public abstract Config findById(int id);

    @Query("SELECT * FROM Config WHERE type = :type ORDER BY time DESC LIMIT 1")
    public abstract Config findOne(int type);

    @Query("SELECT * FROM Config WHERE url = :url AND type = :type")
    public abstract Config find(String url, int type);

    @Query("DELETE FROM Config WHERE url = :url AND type = :type")
    public abstract void delete(String url, int type);

    @Query("DELETE FROM Config WHERE url = :url")
    public abstract void delete(String url);

    @Query("DELETE FROM Config WHERE type = :type")
    public abstract void delete(int type);
}
