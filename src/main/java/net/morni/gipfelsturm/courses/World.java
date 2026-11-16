package net.morni.gipfelsturm.courses;

import java.util.List;
import java.util.Optional;
import net.morni.gipfelsturm.course.Course;
import net.morni.gipfelsturm.course.Plan;

/** Die ganze Gipfelsturm-Welt aus dem Code: Bauplan, Basislager und Strecken. */
public record World(Plan plan, Basecamp basecamp, List<Course> courses) {

    public static World create() {
        Plan plan = new Plan();
        List<Course> courses = CourseCatalog.build(plan);
        Basecamp camp = new Basecamp();
        camp.build(plan, courses);
        return new World(plan, camp, List.copyOf(courses));
    }

    public Optional<Course> course(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String want = id.toLowerCase(java.util.Locale.ROOT);
        return courses.stream().filter(c -> c.id.equals(want) || c.nameDe.equalsIgnoreCase(id)
                || c.nameEn.equalsIgnoreCase(id)).findFirst();
    }

    /** Strecke, in deren Gegend die Position liegt (null = Basislager oder nirgends). */
    public Course near(double x, double y, double z) {
        for (Course c : courses) {
            if (c.bounds.contains(x, y, z)) {
                return c;
            }
        }
        return null;
    }
}
