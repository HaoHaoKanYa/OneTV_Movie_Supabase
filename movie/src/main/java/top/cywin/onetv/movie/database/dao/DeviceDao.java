package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.movie.bean.Device;

import java.util.List;

/**
 * 设备DAO
 * 基于FongMi_TV的DeviceDao完整移植
 */
@Dao
public abstract class DeviceDao extends BaseDao<Device> {

    @Query("SELECT * FROM Device")
    public abstract List<Device> findAll();

    @Query("DELETE FROM Device")
    public abstract void delete();
}
