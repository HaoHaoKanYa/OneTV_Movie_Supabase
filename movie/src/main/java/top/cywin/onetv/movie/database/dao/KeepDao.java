package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.Keep;

import java.util.List;

/**
 * 收藏DAO
 * 基于FongMi_TV的KeepDao完整移植
 */
@Dao
public abstract class KeepDao extends BaseDao<Keep> {

    @Query("SELECT * FROM Keep")
    public abstract List<Keep> findAll();

    @Query("SELECT * FROM Keep WHERE type = 0 ORDER BY createTime DESC")
    public abstract List<Keep> getVod();

    @Query("SELECT * FROM Keep WHERE type = 1 ORDER BY createTime DESC")
    public abstract List<Keep> getLive();

    @Query("SELECT * FROM Keep WHERE type = 0 AND cid = :cid AND `key` = :key")
    public abstract Keep find(int cid, String key);

    @Query("SELECT * FROM Keep WHERE type = 1 AND `key` = :key")
    public abstract Keep find(String key);

    @Query("DELETE FROM Keep WHERE type = 1 AND `key` = :key")
    public abstract void delete(String key);

    @Query("DELETE FROM Keep WHERE type = 0 AND cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM Keep WHERE type = 0 AND cid = :cid")
    public abstract void delete(int cid);

    @Query("DELETE FROM Keep WHERE type = 0")
    public abstract void delete();
}
