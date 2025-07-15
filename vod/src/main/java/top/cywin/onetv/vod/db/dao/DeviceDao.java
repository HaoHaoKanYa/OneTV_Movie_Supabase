package top.cywin.onetv.vod.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.vod.bean.Device;

import java.util.List;

@Dao
public abstract class DeviceDao extends BaseDao<Device> {

    @Query("SELECT * FROM Device")
    public abstract List<Device> findAll();

    @Query("DELETE FROM Device")
    public abstract void delete();
}
