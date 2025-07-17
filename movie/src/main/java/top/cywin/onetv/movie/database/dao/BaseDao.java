package top.cywin.onetv.movie.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础DAO
 * 基于FongMi_TV的BaseDao完整移植
 */
@Dao
public abstract class BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract Long insert(T item);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract List<Long> insert(List<T> items);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(T item);

    @Update
    public abstract void update(List<T> items);

    @Transaction
    public void insertOrUpdate(T item) {
        long id = insert(item);
        if (id == -1) update(item);
    }

    @Transaction
    public void insertOrUpdate(List<T> items) {
        if (items.isEmpty()) return;
        List<Long> result = insert(items);
        List<T> list = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) if (result.get(i) == -1) list.add(items.get(i));
        if (!list.isEmpty()) update(list);
    }
}
