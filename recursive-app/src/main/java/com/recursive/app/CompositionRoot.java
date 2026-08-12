package com.recursive.app;

import com.recursive.application.CachingGlossaryService;
import com.recursive.application.DocumentParsingService;
import com.recursive.application.JobOrchestrator;
import com.recursive.application.LogService;
import com.recursive.application.ModelService;
import com.recursive.application.OpenAICompatibleModelService;
import com.recursive.application.PDFExportService;
import com.recursive.application.RemoteModelDiscoveryService;
import com.recursive.application.StandardModelService;
import com.recursive.application.TranslationOrchestrator;
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
    private final JobOrchestrator jobOrchestrator;
    private final ModelService modelService;
    private final StandardModelService standardModelService;
    private final OpenAICompatibleModelService openAiCompatibleModelService;
    private final RemoteModelDiscoveryService remoteModelDiscovery;
    private final DocumentParsingService parsingService;
    private final TranslationOrchestrator translationOrchestrator;
    private final PDFExportService exportService;

    private CompositionRoot(AppConfig config, DatabaseInitializer database, LogService logService,
                            OllamaModelProvider ollama, SystemMonitor hardware,
                            JobOrchestrator jobOrchestrator, ModelService modelService,
                            StandardModelService standardModelService,
                            OpenAICompatibleModelService openAiCompatibleModelService,
                            RemoteModelDiscoveryService remoteModelDiscovery,
                            DocumentParsingService parsingService,
                            TranslationOrchestrator translationOrchestrator,
                            PDFExportService exportService) {
        this.config = config;
        this.database = database;
        this.logService = logService;
        this.ollama = ollama;
        this.hardware = hardware;
        this.jobOrchestrator = jobOrchestrator;
        this.modelService = modelService;
        this.standardModelService = standardModelService;
        this.openAiCompatibleModelService = openAiCompatibleModelService;
        this.remoteModelDiscovery = remoteModelDiscovery;
        this.parsingService = parsingService;
        this.translationOrchestrator = translationOrchestrator;
        this.exportService = exportService;
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
                ollama, validator, blocks, pages, jobs, glossaryService);
        PDFExportService exportService = new PDFExportService(
                new PdfReconstructor(pages, blocks, images), config.storagePaths());

        return new CompositionRoot(config, database, logService, ollama, hardware,
                jobOrchestrator, modelService, standardModelService,
                openAiCompatibleModelService, remoteModelDiscovery,
                parsingService, translationOrchestrator, exportService);
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

    public TranslationOrchestrator translationOrchestrator() {
        return translationOrchestrator;
    }

    public PDFExportService exportService() {
        return exportService;
    }

    public void close() {
        database.close();
    }
}
