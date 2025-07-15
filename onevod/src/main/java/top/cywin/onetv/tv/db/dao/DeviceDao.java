package top.cywin.onetv.tv.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import top.cywin.onetv.tv.bean.Device;

import java.util.List;

@Dao
public abstract class DeviceDao extends BaseDao<Device> {

    @Query("SELECT * FROM Device")
    public abstract List<Device> findAll();

    @Query("DELETE FROM Device")
    public abstract void delete();
}
