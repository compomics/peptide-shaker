package eu.isas.peptideshaker.scoring.maps.specific;

import com.compomics.util.db.object.ObjectsDB;
import eu.isas.peptideshaker.scoring.maps.SpecificTargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Modification specific target decoy map.
 *
 * @author Marc Vaudel
 */
public class ModificationSpecificMap extends SpecificTargetDecoyMap {

    /**
     * The name of the garbage category.
     */
    public final static String GARBAGE = "OTHER";
    /**
     * The key of the garbage category.
     */
    public final static int GARBAGE_KEY = GARBAGE.hashCode();

    @Override
    public void clean(double minimalFDR) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        for (Map.Entry<Integer, HashMap<String, TargetDecoyMap>> entry1 : fileSpecificMaps.entrySet()) {

            HashMap<String, TargetDecoyMap> subMap = entry1.getValue();
            HashSet<String> suspiciousFiles = new HashSet<>(0);
            TargetDecoyMap tempMap = new TargetDecoyMap();

            for (Map.Entry<String, TargetDecoyMap> entry2 : subMap.entrySet()) {

                TargetDecoyMap targetDecoyMap = entry2.getValue();

                if (targetDecoyMap.suspiciousInput(minimalFDR)) {

                    suspiciousFiles.add(entry2.getKey());
                    tempMap.addAll(targetDecoyMap);

                }
            }

            if (!suspiciousFiles.isEmpty()) {

                int category = entry1.getKey();
                groupedMaps.put(category, tempMap);
                fileSpecificGrouping.put(category, suspiciousFiles);

                if (tempMap.suspiciousInput(minimalFDR)) {

                    TargetDecoyMap garbageMap = groupedMaps.get(GARBAGE_KEY);

                    if (garbageMap == null) {

                        garbageMap = new TargetDecoyMap();
                        groupedMaps.put(GARBAGE_KEY, garbageMap);

                    }

                    garbageMap.addAll(tempMap);
                    grouping.put(category, GARBAGE_KEY);

                }
            }
        }
    }
}
