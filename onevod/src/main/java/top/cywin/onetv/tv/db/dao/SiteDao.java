package top.cywin.onetv.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.tv.bean.Site;

import java.util.List;

@Dao
public abstract class SiteDao extends BaseDao<Site> {

    @Query("SELECT * FROM Site")
    public abstract List<Site> findAll();

    @Query("SELECT * FROM Site WHERE `key` = :key")
    public abstract Site find(String key);
}
