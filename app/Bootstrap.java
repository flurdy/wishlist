import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job {
    public void doJob() {
        // future pre loading of data eg rates
    }
}
