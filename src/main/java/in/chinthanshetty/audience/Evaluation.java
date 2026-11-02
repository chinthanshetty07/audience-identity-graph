package in.chinthanshetty.audience;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pairwise precision and recall of the resolved identities against the generator's ground truth. */
public final class Evaluation {
    public record Scores(long truePairs, long predictedPairs, long correctPairs, double precision, double recall) {}

    private Evaluation() {}

    public static Scores score(List<Synthetic.Labelled> data, List<Identity> identities) {
        Map<String, Integer> person = new HashMap<>();
        data.forEach(l -> person.put(l.record().source() + ":" + l.record().sourceId(), l.personId()));
        Map<String, String> cluster = new HashMap<>();
        identities.forEach(i -> i.sourceIds().forEach(s -> cluster.put(s, i.id())));
        List<String> ids = List.copyOf(person.keySet());
        long tp = 0, fp = 0, fn = 0;
        for (int a = 0; a < ids.size(); a++) {
            for (int b = a + 1; b < ids.size(); b++) {
                boolean same = person.get(ids.get(a)).equals(person.get(ids.get(b)));
                boolean linked = cluster.get(ids.get(a)).equals(cluster.get(ids.get(b)));
                if (same && linked) tp++;
                else if (linked) fp++;
                else if (same) fn++;
            }
        }
        double p = tp + fp == 0 ? 1 : (double) tp / (tp + fp), r = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        return new Scores(tp + fn, tp + fp, tp, p, r);
    }
}
