package com.opswat.metadefender.core.service;

import com.opswat.metadefender.core.client.MetadefenderCoreClient;
import com.opswat.metadefender.core.client.exceptions.MetadefenderClientException;
import com.opswat.metadefender.core.client.responses.*;
import com.opswat.metadefender.core.client.FileScanOptions;

//import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MetaDefenderClientService {
    /**
     *
     * @param apiUrl
     * @param file
     * @return true if file is allowed, otherwise MetadefenderClientException is thrown with detailed message
     * @throws MetadefenderClientException
     */
    public static boolean isFileAllowed(String apiUrl, String file) throws MetadefenderClientException {
        boolean allowed = false;
        MetadefenderCoreClient metadefenderCoreClient = new MetadefenderCoreClient(apiUrl);

        try {
            InputStream inputStream = new FileInputStream(file);
            FileScanResult result = metadefenderCoreClient.scanFileSync(inputStream, new FileScanOptions().setFileName(getFileNameFromPath(file)), 200, 5000);
            if ("Allowed".equalsIgnoreCase(result.process_info.result)) {
                return true;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("\nFile (" + file + ") scan finished with result: " + result.process_info.result);
                //sb.append("\n\t data_id='" + result.data_id);
                ScanResults res = result.scan_results;
                sb.append("\n\tScanResults{" +
                        "data_id='" + res.data_id + '\'' +
                        ", progress_percentage=" + res.progress_percentage +
                        ", scan_all_result_a='" + res.scan_all_result_a + '\'' +
                        ", scan_all_result_i=" + res.scan_all_result_i +
                        ", scan_details=" + res.scan_details +
                        ", start_time=" + res.start_time +
                        ", total_avs=" + res.total_avs +
                        ", total_time=" + res.total_time +
                        '}');
                Map<String, EngineScanDetail> map = res.scan_details;
                sb.append("\n\tEngineScanDetail:");
                for (Map.Entry<String, EngineScanDetail> entry : map.entrySet()) {
                    EngineScanDetail esd = entry.getValue();
                    sb.append("\n\t - Item: " + entry.getKey());
                    sb.append("\n\t - EngineScanDetail{" +
                            "def_time=" + esd.def_time +
                            ", location='" + esd.location + '\'' +
                            ", scan_result_i=" + esd.scan_result_i +
                            ", scan_time=" + esd.scan_time +
                            ", threat_found='" + esd.threat_found + '\'' +
                            '}');
                }
                throw new MetadefenderClientException(sb.toString());
            }//

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + file + " Exception: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new MetadefenderClientException("InterruptedException: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new MetadefenderClientException("ExecutionException: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new MetadefenderClientException("TimeoutException: " + e.getMessage());
        }
        return allowed;
    }

    /**
     *
     * @param apiUrl
     * @param file
     * @return File Scan details if file is allowed, otherwise MetadefenderClientException is thrown with detailed message
     * @throws MetadefenderClientException
     */
    public static String scanFile(String apiUrl, String file) throws MetadefenderClientException {
        MetadefenderCoreClient metadefenderCoreClient = new MetadefenderCoreClient(apiUrl);
        StringBuilder sb = new StringBuilder();
        try {
            InputStream inputStream = new FileInputStream(file);
            FileScanResult result = metadefenderCoreClient.scanFileSync(inputStream, new FileScanOptions().setFileName(getFileNameFromPath(file)), 200, 5000);
            sb.append("\nFile (" + file + ") scan finished with result: " + result.process_info.result);
            //sb.append("\n\t data_id='" + result.data_id);
            ScanResults res = result.scan_results;
            sb.append("\n\tScanResults{" +
                    "data_id='" + res.data_id + '\'' +
                    ", progress_percentage=" + res.progress_percentage +
                    ", scan_all_result_a='" + res.scan_all_result_a + '\'' +
                    ", scan_all_result_i=" + res.scan_all_result_i +
                    ", scan_details=" + res.scan_details +
                    ", start_time=" + res.start_time +
                    ", total_avs=" + res.total_avs +
                    ", total_time=" + res.total_time +
                    '}');
            Map<String, EngineScanDetail> map = res.scan_details;
            sb.append("\n\tEngineScanDetail:");
            for (Map.Entry<String, EngineScanDetail> entry : map.entrySet()) {
                EngineScanDetail esd = entry.getValue();
                sb.append("\n\t - Item: " + entry.getKey());
                sb.append("\t EngineScanDetail{" +
                        "def_time=" + esd.def_time +
                        ", location='" + esd.location + '\'' +
                        ", scan_result_i=" + esd.scan_result_i +
                        ", scan_time=" + esd.scan_time +
                        ", threat_found='" + esd.threat_found + '\'' +
                        '}');
            }
            if (!"Allowed".equalsIgnoreCase(result.process_info.result)) {
                throw new MetadefenderClientException(sb.toString());
            }

        } catch (FileNotFoundException e) {
            throw new MetadefenderClientException("File not found: " + file + " Exception: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new MetadefenderClientException("InterruptedException: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new MetadefenderClientException("ExecutionException: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new MetadefenderClientException("TimeoutException: " + e.getMessage());
        }
        return sb.toString();
    }


    /**
     *
     * @param apiUrl
     * @param apiUser
     * @param apiUserPass
     */
    public static String showApiInfo(String apiUrl, String apiUser, String apiUserPass) {
        StringBuilder sb = new StringBuilder();
        MetadefenderCoreClient metadefenderCoreClient;
        try {
            metadefenderCoreClient = new MetadefenderCoreClient(apiUrl, apiUser, apiUserPass);
            sb.append("Metadefender client created. Session id is: " + metadefenderCoreClient.getSessionId());

            ApiVersion apiVersion = metadefenderCoreClient.getVersion();
            sb.append("\nApiVersion: {");
            sb.append("Version: " + apiVersion.version);
            sb.append(", product_id: " + apiVersion.product_id + "}");

            License license = metadefenderCoreClient.getCurrentLicenseInformation();
            sb.append("\nLicense: {");
            sb.append("Licensed to: " + license.licensed_to);
            sb.append(", expiration: " + license.expiration);
            sb.append(", product_id: " + license.product_id);
            sb.append(", tproduct_name: " + license.product_name);
            sb.append(", licensed_engines: " + license.licensed_engines);
            sb.append(", online_activated: " + license.online_activated+"}");

            List<EngineVersion> result = metadefenderCoreClient.getEngineVersions();
            sb.append("\nEngine/database Version: {");
            for(EngineVersion ev: result) {
                sb.append("eng_id: " + ev.eng_id);
                sb.append(", eng_name: " + ev.eng_name);
                sb.append(", eng_type: " + ev.eng_type);
                sb.append(", eng_ver: " + ev.eng_ver);
                sb.append(", engine_type: " + ev.engine_type);
                sb.append(", state: " + ev.state);
                sb.append(", active: " + ev.active);
                sb.append(", def_time: " + ev.def_time);
                sb.append(", download_time: " + ev.download_time);
            }

            List<ScanRule> scanRules = metadefenderCoreClient.getAvailableScanRules();
            sb.append("\nAvailable scan rules: {" + scanRules.size());
            for (ScanRule rule : scanRules) {
                sb.append("scan rule name: " + rule.name + ", ");
            }
            sb.append("} ");
            metadefenderCoreClient.logout();
            sb.append("\n***Client successfully logged out.");
		} catch (MetadefenderClientException e) {
            sb.append("Error during show api info: " + e.getDetailedMessage());
        }
        return sb.toString();
    }

    private static String getFileNameFromPath(String file) {
        String parts[];
        if(file.contains("/")) {
            parts = file.split("/");
        } else if(file.contains("\\")) {
            parts = file.split("\\\\");
        } else {
               return file;
        }

        if(parts.length > 1) {
            return parts[parts.length - 1];
        }
        return file;
    }
}
