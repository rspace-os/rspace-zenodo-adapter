package com.researchspace.zenodo.rspaceadapter;

import static com.researchspace.zenodo.rspaceadapter.ZenodoRSpaceRepository.RAID_METADATA_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.zenodo.client.ZenodoClient;
import com.researchspace.zenodo.model.RelatedIdentifier;
import com.researchspace.zenodo.model.ZenodoDeposition;
import com.researchspace.zenodo.model.ZenodoFile;
import com.researchspace.zenodo.model.ZenodoSubmission;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ZenodoRSpaceRepositoryTest {

  @Mock
  private ZenodoClient zenodoClient;
  @InjectMocks
  private ZenodoRSpaceRepository repoAdapter;
  @Mock
  private IDepositor author;
  private ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  public void setUp() throws MalformedURLException {
    MockitoAnnotations.openMocks(this);
    RepositoryConfig config = new RepositoryConfig(new URL("https://sandbox.zenodo.org/api"),
        "<api-token>", null, "app.zenodo");
    repoAdapter = new ZenodoRSpaceRepository();
    repoAdapter.configure(config);
    repoAdapter.setZenodoClient(zenodoClient);
  }

  @Test
  void testTestConnection() throws IOException {
    when(zenodoClient.getDepositions()).thenReturn(Collections.emptyList());
    assertTrue(repoAdapter.testConnection().isSucceeded());
  }

  @Test
  void testSubmitDeposit() throws IOException {
    ZenodoSubmission expectedSubmission = getExpectedZenodoSubmission();

    when(zenodoClient.createDeposition(expectedSubmission)).thenReturn(
        getZenodoDeposition());
    when(zenodoClient.depositFile(any(ZenodoDeposition.class), any(String.class),
        any(File.class))).thenReturn(getZenodoFile());
    SubmissionMetadata metadata = getTestSubmissionMetadata();
    File file = new File("src/test/resources/test.txt");
    RepositoryOperationResult dataset = repoAdapter.submitDeposit(null, file, metadata, null);
    assertTrue(dataset.isSucceeded());
  }

  private static ZenodoSubmission getExpectedZenodoSubmission() {
    return new ZenodoSubmission("title", "desc", "other", true,
        List.of(new RelatedIdentifier("10.5072/zenodo.1059996", "isDocumentedBy",
                "publication-datamanagementplan"),
            new RelatedIdentifier("https://raid.org/10.12345/NICO26", "isPartOf", "other")),
        new LinkedList<>());
  }

  private ZenodoFile getZenodoFile() throws IOException {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(
        IOUtils.resourceToString("/fileDepositResponse.json", StandardCharsets.UTF_8),
        ZenodoFile.class);
  }

  private ZenodoDeposition getZenodoDeposition() throws IOException {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(
        IOUtils.resourceToString("/depositionCreationResponse.json", StandardCharsets.UTF_8),
        ZenodoDeposition.class);
  }

  private SubmissionMetadata getTestSubmissionMetadata() throws IOException {
    SubmissionMetadata md = new SubmissionMetadata();
    when(author.getEmail()).thenReturn("email@somewhere.com");
    when(author.getUniqueName()).thenReturn("anyone");

    md.setAuthors(List.of(author));
    md.setContacts(List.of(author));
    md.setDescription("desc");
    md.setPublish(false);
    md.setSubjects(List.of("Other natural sciences"));
    md.setLicense(Optional.of(new URL("https://creativecommons.org/publicdomain/zero/1.0/")));
    md.setTitle("title");
    md.setDmpDoi(Optional.of("10.5072/zenodo.1059996"));
    md.setOtherProperties(Map.of(RAID_METADATA_PROPERTY, "https://raid.org/10.12345/NICO26"));

    return md;
  }

}
