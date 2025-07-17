package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.History;

import java.util.List;

/**
 * 历史记录DAO
 * 基于FongMi_TV的HistoryDao完整移植
 */
@Dao
public abstract class HistoryDao extends BaseDao<History> {

    @Query("SELECT * FROM History")
    public abstract List<History> findAll();

    @Query("SELECT * FROM History WHERE cid = :cid AND createTime >= :createTime ORDER BY createTime DESC")
    public abstract List<History> find(int cid, long createTime);

    @Query("SELECT * FROM History WHERE cid = :cid AND `key` = :key")
    public abstract History find(int cid, String key);

    @Query("SELECT * FROM History WHERE cid = :cid AND vodName = :vodName")
    public abstract List<History> findByName(int cid, String vodName);

    @Query("DELETE FROM History WHERE cid = :cid AND `key` = :key")
    public abstract void delete(int cid, String key);

    @Query("DELETE FROM History WHERE cid = :cid")
    public abstract void delete(int cid);

    @Query("DELETE FROM History")
    public abstract void delete();
}
