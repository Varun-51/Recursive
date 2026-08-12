package com.recursive.app;

import com.recursive.application.CachingGlossaryService;
import com.recursive.application.CompletionEstimator;
import com.recursive.application.DocumentParsingService;
import com.recursive.application.JobOrchestrator;
import com.recursive.application.LogService;
import com.recursive.application.ModelService;
import com.recursive.application.OpenAICompatibleModelService;
import com.recursive.application.PDFExportService;
import com.recursive.application.RemoteModelDiscoveryService;
import com.recursive.application.StandardModelService;
import com.recursive.application.ThroughputController;
import com.recursive.application.TranslationOrchestrator;
import com.recursive.application.TranslationRunner;
import com.recursive.infrastructure.appconfig.AppConfig;
import com.recursive.infrastructure.database.DatabaseInitializer;
import com.recursive.infrastructure.database.JdbcBlockRepository;
import com.recursive.infrastructure.database.JdbcGlossaryRepository;
import com.recursive.infrastructure.database.JdbcImageRepository;
import com.recursive.infrastructure.database.JdbcJobRepository;
import com.recursive.infrastructure.database.JdbcPageRepository;
import com.recursive.infrastructure.extraction.PdfDocumentParser;
import com.recursive.infrastructure.llm.OllamaHttpClient;
import com.recursive.infrastructure.llm.OllamaModelProvider;
import com.recursive.infrastructure.llm.OpenAiCompatibleCatalog;
import com.recursive.infrastructure.logging.Slf4jLogListener;
import com.recursive.infrastructure.ocr.TesseractOcrEngine;
import com.recursive.infrastructure.pdf.PdfReconstructor;
import com.recursive.infrastructure.system.SystemMonitor;
import com.recursive.infrastructure.verification.AdaptiveFlowValidator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Assembles the application's services against the infrastructure
 * implementations. Created once at startup, closed once at shutdown; every
 * collaborating object is constructor-injected from here.
 */
public final class CompositionRoot {

    private final AppConfig config;
    private final DatabaseInitializer database;
    private final LogService logService;
    private final OllamaModelProvider ollama;
    private final SystemMonitor hardware;
    private final ThroughputController throughput;
    private final ExecutorService translationWorkers;
    private final CompletionEstimator estimator;
    private final JobOrchestrator jobOrchestrator;
    private final ModelService modelService;
    private final StandardModelService standardModelService;
    private final OpenAICompatibleModelService openAiCompatibleModelService;
    private final RemoteModelDiscoveryService remoteModelDiscovery;
    private final DocumentParsingService parsingService;
    private final TranslationRunner translationRunner;
    private final PDFExportService exportService;
    private final JdbcJobRepository jobs;
    private final JdbcPageRepository pages;
    private final JdbcBlockRepository blocks;
    private final JdbcGlossaryRepository glossary;

    private CompositionRoot(AppConfig config, DatabaseInitializer database, LogService logService,
                            OllamaModelProvider ollama, SystemMonitor hardware,
                            ThroughputController throughput, ExecutorService translationWorkers,
                            CompletionEstimator estimator,
                            JobOrchestrator jobOrchestrator, ModelService modelService,
                            StandardModelService standardModelService,
                            OpenAICompatibleModelService openAiCompatibleModelService,
                            RemoteModelDiscoveryService remoteModelDiscovery,
                            DocumentParsingService parsingService,
                            TranslationRunner translationRunner,
                            PDFExportService exportService,
                            JdbcJobRepository jobs, JdbcPageRepository pages,
                            JdbcBlockRepository blocks, JdbcGlossaryRepository glossary) {
        this.config = config;
        this.database = database;
        this.logService = logService;
        this.ollama = ollama;
        this.hardware = hardware;
        this.throughput = throughput;
        this.translationWorkers = translationWorkers;
        this.estimator = estimator;
        this.jobOrchestrator = jobOrchestrator;
        this.modelService = modelService;
        this.standardModelService = standardModelService;
        this.openAiCompatibleModelService = openAiCompatibleModelService;
        this.remoteModelDiscovery = remoteModelDiscovery;
        this.parsingService = parsingService;
        this.translationRunner = translationRunner;
        this.exportService = exportService;
        this.jobs = jobs;
        this.pages = pages;
        this.blocks = blocks;
        this.glossary = glossary;
    }

    public static CompositionRoot build(AppConfig config) {
        DatabaseInitializer database = new DatabaseInitializer(config.databaseFile());
        database.initialize();
        LogService logService = new LogService();
        Slf4jLogListener.attachTo(logService);

        JdbcJobRepository jobs = new JdbcJobRepository(database.connectionProvider());
        JdbcPageRepository pages = new JdbcPageRepository(database.connectionProvider());
        JdbcBlockRepository blocks = new JdbcBlockRepository(database.connectionProvider());
        JdbcImageRepository images = new JdbcImageRepository(database.connectionProvider());
        JdbcGlossaryRepository glossary = new JdbcGlossaryRepository(database.connectionProvider());

        OllamaModelProvider ollama = new OllamaModelProvider(new OllamaHttpClient(config.ollamaBaseUrl()));
        SystemMonitor hardware = new SystemMonitor();
        ThroughputController throughput = ThroughputController.polling(hardware);
        ExecutorService translationWorkers = Executors.newVirtualThreadPerTaskExecutor();
        CompletionEstimator estimator = new CompletionEstimator();
        AdaptiveFlowValidator validator = new AdaptiveFlowValidator();
        CachingGlossaryService glossaryService = new CachingGlossaryService(glossary);

        JobOrchestrator jobOrchestrator = new JobOrchestrator(jobs);
        ModelService modelService = new ModelService(ollama, hardware);
        StandardModelService standardModelService = new StandardModelService(ollama);
        OpenAICompatibleModelService openAiCompatibleModelService =
                new OpenAICompatibleModelService(new OpenAiCompatibleCatalog());
        RemoteModelDiscoveryService remoteModelDiscovery =
                new RemoteModelDiscoveryService(new OpenAiCompatibleCatalog());
        DocumentParsingService parsingService = new DocumentParsingService(
                PdfDocumentParser.create(), new TesseractOcrEngine(), pages, blocks, images);
        TranslationOrchestrator translationOrchestrator = new TranslationOrchestrator(
                ollama, validator, blocks, glossaryService);
        TranslationRunner translationRunner = new TranslationRunner(
                translationOrchestrator, throughput, translationWorkers, blocks, pages, jobs);
        PDFExportService exportService = new PDFExportService(
                new PdfReconstructor(pages, blocks, images), config.storagePaths());

        return new CompositionRoot(config, database, logService, ollama, hardware,
                throughput, translationWorkers, estimator,
                jobOrchestrator, modelService, standardModelService,
                openAiCompatibleModelService, remoteModelDiscovery,
                parsingService, translationRunner, exportService,
                jobs, pages, blocks, glossary);
    }

    public AppConfig config() {
        return config;
    }

    public LogService logService() {
        return logService;
    }

    public OllamaModelProvider ollama() {
        return ollama;
    }

    public SystemMonitor hardware() {
        return hardware;
    }

    public ThroughputController throughput() {
        return throughput;
    }

    public CompletionEstimator estimator() {
        return estimator;
    }

    public JobOrchestrator jobOrchestrator() {
        return jobOrchestrator;
    }

    public ModelService modelService() {
        return modelService;
    }

    public StandardModelService standardModelService() {
        return standardModelService;
    }

    public OpenAICompatibleModelService openAiCompatibleModelService() {
        return openAiCompatibleModelService;
    }

    public RemoteModelDiscoveryService remoteModelDiscovery() {
        return remoteModelDiscovery;
    }

    public DocumentParsingService parsingService() {
        return parsingService;
    }

    public TranslationRunner translationRunner() {
        return translationRunner;
    }

    public PDFExportService exportService() {
        return exportService;
    }

    public JdbcJobRepository jobs() {
        return jobs;
    }

    public JdbcPageRepository pages() {
        return pages;
    }

    public JdbcBlockRepository blocks() {
        return blocks;
    }

    public JdbcGlossaryRepository glossary() {
        return glossary;
    }

    public void close() {
        throughput.close();
        translationWorkers.close();
        database.close();
    }
}
