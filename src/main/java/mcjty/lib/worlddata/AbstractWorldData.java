package mcjty.lib.worlddata;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWorldData<T extends AbstractWorldData> extends WorldSavedData {

    private static final Map<String, AbstractWorldData<?>> instances = new HashMap<>();

    protected AbstractWorldData(String name) {
        super(name);
    }

    public void save(World world) {
        world.setData(mapName, this);
        markDirty();
    }

    public abstract void clear();

    public static void clearInstances() {
        for (AbstractWorldData data : instances.values()) {
            data.clear();
        }
        instances.clear();
    }

    @Nonnull
    public static <T extends AbstractWorldData> T getData(World world, Class<T> clazz, String name) {
        if (world.isRemote) {
            throw new RuntimeException("Don't access this client-side!");
        }

        AbstractWorldData<?> data = instances.get(name);
        if (data != null) {
            return (T) data;
        }

        data = (T) world.loadData(clazz, name);
        if (data == null) {
            try {
                data = clazz.getConstructor(String.class).newInstance(name);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        instances.put(name, data);
        return (T) data;
    }



}
