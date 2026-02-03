package com.researchspace.zenodo.rspaceadapter;


import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.IRepository;
import com.researchspace.repository.spi.LicenseConfigInfo;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryConfigurer;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.Subject;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.repository.spi.properties.RepoProperty;
import com.researchspace.zenodo.client.ZenodoClient;
import com.researchspace.zenodo.client.ZenodoClientImpl;
import com.researchspace.zenodo.model.RelatedIdentifier;
import com.researchspace.zenodo.model.ZenodoDeposition;
import com.researchspace.zenodo.model.ZenodoFile;
import com.researchspace.zenodo.model.ZenodoSubmission;
import com.researchspace.zenodo.utils.ZenodoRSpaceAdapterUtils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;


@Getter
@Setter
@Slf4j
public class ZenodoRSpaceRepository implements IRepository, RepositoryConfigurer {

    private ZenodoClient zenodoClient;


    @Override
    public void configure(RepositoryConfig repositoryConfig) {
        this.zenodoClient = new ZenodoClientImpl(repositoryConfig.getServerURL(), repositoryConfig.getIdentifier());
    }

    @Override
    public RepositoryOperationResult submitDeposit(IDepositor iDepositor, File file, SubmissionMetadata submissionMetadata, RepositoryConfig repositoryConfig) {
        ZenodoSubmission submission = createZenodoSubmission(submissionMetadata);

        try {

            ZenodoDeposition deposition = zenodoClient.createDeposition(submission);
            ZenodoFile depositedFile = zenodoClient.depositFile(deposition, file.getName(), file);
            return new RepositoryOperationResult(true, "Export uploaded to Zenodo successfully.", deposition.getHtmlUrl(), deposition.getDoiUrl());

        } catch (RestClientException e) {
            log.error("RestClientException occurred while submitting to Zenodo", e);
            return new RepositoryOperationResult(false, "RestClientException occurred while submitting to Zenodo", null, null);
        } catch (MalformedURLException e) {
            log.error("MalformedURLException occurred while submitting to Zenodo", e);
            return new RepositoryOperationResult(false, "MalformedURLException occurred while submitting to Zenodo", null, null);
        } catch (IOException e) {
            log.error("IOException occurred while submitting to Zenodo", e);
            return new RepositoryOperationResult(false, "IOException occurred while submitting to Zenodo", null, null);
        }
    }

    private ZenodoSubmission createZenodoSubmission(SubmissionMetadata submissionMetadata) {
        return new ZenodoSubmission(
            submissionMetadata.getTitle(),
            submissionMetadata.getDescription(),
            "other",
            true,
            getDmpDois(submissionMetadata),
            getControlledVocabularyTerms(submissionMetadata)
        );
    }

    private List<com.researchspace.zenodo.model.ControlledVocabularyTerm> getControlledVocabularyTerms(SubmissionMetadata submissionMetadata) {
      var terms = new ArrayList();
      for(com.researchspace.repository.spi.ControlledVocabularyTerm term : submissionMetadata.getTerms()) {
        terms.add(new com.researchspace.zenodo.model.ControlledVocabularyTerm(term.getValue(), term.getUri(), "url"));
      }
      return terms;
    }

    private List<RelatedIdentifier> getDmpDois(SubmissionMetadata submissionMetadata) {
      List<RelatedIdentifier> dmpDois = new ArrayList<>();
      if(submissionMetadata.getDmpDoi().isPresent()) {
        dmpDois.add(new RelatedIdentifier(
          submissionMetadata.getDmpDoi().get(),
          "isDocumentedBy",
          "publication-datamanagementplan"
        ));
      }
      return dmpDois;
    }

    @Override
    public RepositoryOperationResult testConnection() {
        try {
            zenodoClient.getDepositions();
            return new RepositoryOperationResult(true, "Test connection OK!", null, null);
        } catch (RestClientException | IOException e) {
            log.error("Couldn't perform test action {}", e.getMessage());
            return new RepositoryOperationResult(false, "Test connection failed - " + e.getMessage(), null, null);
        }
    }

    @Override
    public RepositoryConfigurer getConfigurer() {
        return this;
    }

    @Override
    public List<Subject> getSubjects() {
        return ZenodoRSpaceAdapterUtils.getZenodoSubjects();
    }

    @SneakyThrows
    @Override
    public LicenseConfigInfo getLicenseConfigInfo() {
        return new LicenseConfigInfo(true, false, ZenodoRSpaceAdapterUtils.getZenodoLicenses());
    }

    @Override
    public Map<String, RepoProperty> getOtherProperties() {
        return new HashMap<>();
    }
}

