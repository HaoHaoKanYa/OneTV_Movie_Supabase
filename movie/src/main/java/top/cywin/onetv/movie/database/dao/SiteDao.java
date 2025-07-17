package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.Site;

import java.util.List;

/**
 * 站点DAO
 * 基于FongMi_TV的SiteDao完整移植
 */
@Dao
public abstract class SiteDao extends BaseDao<Site> {

    @Query("SELECT * FROM Site")
    public abstract List<Site> findAll();

    @Query("SELECT * FROM Site WHERE `key` = :key")
    public abstract Site find(String key);
}
