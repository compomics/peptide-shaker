package eu.isas.peptideshaker.scoring.maps.specific;

import com.compomics.util.db.object.ObjectsDB;
import eu.isas.peptideshaker.scoring.maps.SpecificTargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Specific target decoy map where the highest value with enough values is kept.
 *
 * @author Marc Vaudel
 */
public class SimpleSpecificMap extends SpecificTargetDecoyMap {

    @Override
    public void clean(double minimalFDR) {

        ObjectsDB.increaseRWCounter();
        zooActivateWrite();
        ObjectsDB.decreaseRWCounter();

        TreeMap<Integer, HashMap<String, TargetDecoyMap>> sortedMap = new TreeMap<>(fileSpecificMaps);

        int refCategory = 0;
        for (Map.Entry<Integer, HashMap<String, TargetDecoyMap>> entry1 : sortedMap.entrySet()) {

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

            if (suspiciousFiles.isEmpty()) {

                refCategory = 0;

            } else {

                int category = entry1.getKey();
                groupedMaps.put(category, tempMap);
                fileSpecificGrouping.put(category, suspiciousFiles);

                if (tempMap.suspiciousInput(minimalFDR)) {

                    if (refCategory > 0) {

                        groupedMaps.get(refCategory).addAll(tempMap);
                        grouping.put(category, refCategory);

                    } else {

                        refCategory = category;

                    }
                } else {

                    refCategory = 0;

                }
            }
        }
    }

}
