package sa.elm.ob.utility.tabadul;

import org.springframework.http.HttpStatus;
/**
 * Checks the error in Rest call
 * @author mrahim
 *
 */
public class TabadulRestUtil {

	public static boolean isError(HttpStatus status) {
        HttpStatus.Series series = status.series();
        return (HttpStatus.Series.CLIENT_ERROR.equals(series)
                || HttpStatus.Series.SERVER_ERROR.equals(series));
    }
}
