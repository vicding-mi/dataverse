package edu.harvard.iq.dataverse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: using unirest here, should use httpclient to reduce extra import
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

// TODO: dirty import here, should move the function generateIdentifier() back to AbstractGlobalIdServiceBean.java
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * @author vicding-mi
 */
@Stateless
public class NbnServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.index.NbnServiceBean");

    // DANS generator url
    URL dansPidGeneratorUrl = null;
    String baseURLString = "";
    private String username = "";
    private String password = "";
    private String apiToken = "";
    private String fqdn = "";
    private String baseMetadataUrl = "";
    private String bridgeApiKey = "";

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public NbnServiceBean() throws MalformedURLException {
        // DANS generator url
        dansPidGeneratorUrl = new URL("http://localhost:20140");
        baseURLString = System.getProperty("nbn.baseurlstring"); // TODO: check if baseurlstring is needed at all
        username = System.getProperty("nbn.username");
        password = System.getProperty("nbn.password");
        apiToken = System.getProperty("dataverseAdmin.apitoken");
        bridgeApiKey = System.getProperty("bridge.api.key");
        logger.log(Level.INFO, "Using baseURLString {0}", baseURLString);
        logger.log(Level.INFO, "Using bridgeApiKey {0}", bridgeApiKey);

        //we should use  systemConfig.getDataverseSiteUrl()
        //but it will force you to use https while we don't use https except on production.
        baseMetadataUrl = System.getProperty("dataverse.metadata.url");
    }

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String identifier) {
        logger.log(Level.FINE,"getIdentifierForLookup");
        return protocol + ":" + authority + ":" + identifier;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public boolean alreadyExists(DvObject dvObject) throws Exception {
        logger.log(Level.FINE, "alreadyExists");
        // String identifier = getIdentifier(dvObject);
        /* Not checking the uniqueness since we do not want a remote registry to URN:NBN
           return false directly
        */
        return false;
    }

    @Override
    public boolean alreadyExists(GlobalId globalId) throws Exception {
        logger.log(Level.FINE, "alreadyExists");
        return false;
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvObject) {
        throw new NotImplementedException();
    }

    @Override
    public HashMap lookupMetadataFromIdentifier(String protocol, String authority, String identifier) {
        throw new NotImplementedException();
    }

    @Override
    public boolean publicizeIdentifier(DvObject dvObject) {
        logger.log(Level.FINE, "updateIdentifierStatus");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            dvObject = generateIdentifier(dvObject);
        }

        return true;
    }

    @Override
    public List<String> getProviderInformation() {
        ArrayList<String> providerInfo = new ArrayList<>();
        String providerName = "URN:NBN";
        String providerLink = baseURLString;
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }

    public boolean isIdentifierLocallyUnique(String identifier, Dataset dataset) {
        return em.createNamedQuery("Dataset.findByIdentifierAuthorityProtocol")
                .setParameter("identifier", identifier)
                .setParameter("authority", dataset.getAuthority())
                .setParameter("protocol", dataset.getProtocol())
                .getResultList().isEmpty();
    }

    /**
     * Generating DANS PID
     * the service is returning full urn:nbn PID
     * we have to strip off the prefixes
     *
     * @param url
     * @return identifier
     */
    private String generateIdentifierDANS(URL url, Dataset dataset, String shoulder) throws UnirestException, MalformedURLException {
        HttpResponse<String> response;
        String prefix = dataset.getProtocol() + ":" + dataset.getAuthority() + ":" + shoulder;
        logger.severe("### prefix for DANS pid: " + prefix);
        url = new URL(url, "/create?type=urn");
        try {
            response = Unirest
                    .post(url.toString())
                    .header("Accept", "*")
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException(String.format("### http call failed with url [%s]", url.toString()), e);
        }

        if (response.getStatus() == 201) {
            String fullIdentifier = response.getBody();
            logger.severe("### new DANS PID generated: " + fullIdentifier);
            String identifier = fullIdentifier.substring(prefix.length());
            logger.severe("### new DANS local id generated: " + identifier);
            return shoulder + identifier;
        }
        throw new UnirestException("### Call to DANS PID generator failed. ");
    }

    public String generateDatasetIdentifier(Dataset dataset, GlobalIdServiceBean idServiceBean) throws UnirestException, MalformedURLException {
        logger.severe("### IdentifierGenerationStype is: " + SettingsServiceBean.Key.IdentifierGenerationStyle);
        String identifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "randomString");
        String shoulder = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder, "");

        switch (identifierType) {
            case "randomString":
                logger.severe("### generateIdentifier with randomString");
                return generateIdentifierDANS(dansPidGeneratorUrl, dataset, shoulder);
            case "sequentialNumber":
                /* Should we throw an exception */
                return "sequentialNumber";
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                // default to DANS generator
                logger.severe("### generateIdentifier with randomString by default");
                return generateIdentifierDANS(dansPidGeneratorUrl, dataset, shoulder);
        }
    }

    @Override
    public DvObject generateIdentifier(DvObject dvObject) {

        String protocol = dvObject.getProtocol() == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Protocol) : dvObject.getProtocol();
        String authority = dvObject.getAuthority() == null ? settingsService.getValueForKey(SettingsServiceBean.Key.Authority) : dvObject.getAuthority();
        logger.severe("### protocol is: " + protocol + "; authority is: " + authority);

        // init only for test
        GlobalIdServiceBean idServiceBean = null;
        try {
            idServiceBean = GlobalIdServiceBean.getBean(protocol, commandEngine.getContext());
        } catch (Exception ex) {
            logger.severe("### cannot getBean");
        }
        if (dvObject.isInstanceofDataset()) {
            String newId = "";
            try {
                newId = generateDatasetIdentifier((Dataset) dvObject, idServiceBean);
            } catch (UnirestException | MalformedURLException e) {
                e.printStackTrace();
            }

            logger.severe("### Generating NBN: " + newId);
//            dvObject.setIdentifier(datasetService.generateDatasetIdentifier((Dataset) dvObject, idServiceBean));
            dvObject.setIdentifier(newId);
        } else {
//            dvObject.setIdentifier(datafileService.generateDataFileIdentifier((DataFile) dvObject, idServiceBean));
//            dvObject.setIdentifier(generateDataFileIdentifier((DataFile) dvObject, idServiceBean));
            logger.severe("### does not support datafile yet");
        }
        if (dvObject.getProtocol() == null) {
            dvObject.setProtocol(protocol);
        }
        if (dvObject.getAuthority() == null) {
            dvObject.setAuthority(authority);
        }
        return dvObject;
    }

    @Override
    public String createIdentifier(DvObject dvObject) throws Throwable {
        logger.severe("### createIdentifier");
        if (dvObject.getIdentifier() == null || dvObject.getIdentifier().isEmpty()) {
            // TODO: generate indentifier according to URN:NBN
            dvObject = generateIdentifier(dvObject);
        } else {
            logger.severe("### identifier existed before we mint it: " + dvObject.getIdentifier());
            logger.severe("### Overwriting the pre-existed PID");
            dvObject = generateIdentifier(dvObject);
        }
        String identifier = getIdentifier(dvObject);
        // TODO: check DOI on what to do here, does not seem logical, what is this retString?
        String retString = "MY-" + RandomStringUtils.randomAlphabetic(8).toUpperCase();
        logger.severe("### Create NBN identifier retString : " + retString);
        return retString;
    }


    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        System.out.println("modifyIdentifierTargetURL");
        return null;
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {

    }


    /**
     * Returns a HashMap with the same values as {@code map}. This can be either
     * {@code map} itself, or a new instance with the same values.
     * <p>
     * This is needed as some of the internal APIs here require HashMap, but we
     * don't want the external APIs to use an implementation class.
     *
     * @param <T>
     * @param map
     * @return A HashMap with the same values as {@code map}
     */
    private <T> HashMap<T, T> asHashMap(Map<T, T> map) {
        return (map instanceof HashMap) ? (HashMap) map : new HashMap<>(map);
    }

}

