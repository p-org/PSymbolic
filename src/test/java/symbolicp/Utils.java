package symbolicp;

import symbolicp.runtime.MachineTag;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class Utils {
    public static MachineTag[] getMachineTags(Class<?> wrapper_class, Object wrapper) throws IllegalAccessException {
        ArrayList<MachineTag> machineTags = new ArrayList<>();
        // Collect all machine tag fields from the class
        for (Field field : wrapper_class.getFields()) {
            if (field.getName().startsWith("machineTag")) {
                machineTags.add((MachineTag) field.get(wrapper));
            }
        }
        MachineTag[] machineTags1 = new MachineTag[machineTags.size()];
        machineTags.toArray(machineTags1);
        return machineTags1;
    }
}
