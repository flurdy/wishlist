import models.User;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;

@OnApplicationStart
public class Bootstrap extends Job {
    public void doJob() {
        if(User.count() == 0) {
            Fixtures.deleteAllModels();
            Fixtures.loadModels("initial-data.yml");
        }
    }
}
