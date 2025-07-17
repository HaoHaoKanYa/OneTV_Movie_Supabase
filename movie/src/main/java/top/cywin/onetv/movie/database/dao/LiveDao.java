package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.Live;

import java.util.List;

/**
 * 直播DAO
 * 基于FongMi_TV的LiveDao完整移植
 */
@Dao
public abstract class LiveDao extends BaseDao<Live> {

    @Query("SELECT * FROM Live")
    public abstract List<Live> findAll();

    @Query("SELECT * FROM Live WHERE name = :name")
    public abstract Live find(String name);
}
