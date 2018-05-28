import com.opswat.metadefender.core.client.MetadefenderCoreClient;
import com.opswat.metadefender.core.client.exceptions.MetadefenderClientException;
import com.opswat.metadefender.core.client.responses.FileScanResult;
import com.opswat.metadefender.core.service.MetaDefenderClientService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MetaDefenderClientServiceTest {
    /**
     * @param args command line args
     */
    public static void main(String[] args) {

        String apiUrl =         "http://localhost:8008";
        String apiUser =        "admin";
        String apiUserPass =    "admin";
        String action =         "scan_sync";
        String file = "C:\\train\\metadefenderclientapplication\\src\\test\\java\\testScanFile.txt";
        String hash =           "";

        String apiInfo = MetaDefenderClientService.showApiInfo(apiUrl, apiUser, apiUserPass);
        System.out.println("========================================================================");
        System.out.println(apiInfo);
        System.out.println("========================================================================");
        try {
            String scanResult = MetaDefenderClientService.scanFile(apiUrl, file);
            System.out.println(scanResult);
        } catch (MetadefenderClientException e) {
            System.out.println(e.responseCode);
            System.out.println(e.getDetailedMessage());
        }
        System.out.println("========================================================================");
        try {
            boolean scanResult = MetaDefenderClientService.isFileAllowed(apiUrl, file);
            System.out.println(scanResult);
        } catch (MetadefenderClientException e) {
            System.out.println(e.responseCode);
            System.out.println(e.getDetailedMessage());
        }




    }

}
