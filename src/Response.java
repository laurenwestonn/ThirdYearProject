import java.util.Iterator;
import java.util.List;

public class Response implements Iterable<Result> {
	public List<Result> results;
	public String status;
	
	public List<Result> getElevations() {
		return results;
	}
	
	@Override
	public Iterator<Result> iterator() {
		return results.iterator();
	}
}
