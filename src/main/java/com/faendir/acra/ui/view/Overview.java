package com.faendir.acra.ui.view;

import com.faendir.acra.security.SecurityUtils;
import com.faendir.acra.sql.data.AppRepository;
import com.faendir.acra.sql.data.ReportRepository;
import com.faendir.acra.sql.model.App;
import com.faendir.acra.sql.model.Permission;
import com.faendir.acra.sql.model.User;
import com.faendir.acra.sql.user.UserManager;
import com.faendir.acra.ui.view.base.ConfigurationLabel;
import com.faendir.acra.ui.view.base.MyGrid;
import com.faendir.acra.ui.view.base.NamedView;
import com.faendir.acra.util.BufferedDataProvider;
import com.faendir.acra.util.Style;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;

import java.util.function.Predicate;

/**
 * @author Lukas
 * @since 23.03.2017
 */
@SpringView(name = "")
public class Overview extends NamedView {
    @NonNull private final AppRepository appRepository;
    @NonNull private final ReportRepository reportRepository;
    @NonNull private final UserManager userManager;
    private MyGrid<App> grid;

    @Autowired
    public Overview(@NonNull AppRepository appRepository, @NonNull ReportRepository reportRepository, @NonNull UserManager userManager) {
        this.appRepository = appRepository;
        this.reportRepository = reportRepository;
        this.userManager = userManager;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        ConfigurableFilterDataProvider<App, Void, Predicate<App>> dataProvider = new BufferedDataProvider<>(appRepository::findAll,
                                                                                                            () -> Math.toIntExact(appRepository.count())).withConfigurableFilter();
        dataProvider.setFilter(app -> SecurityUtils.hasPermission(app, Permission.Level.VIEW));
        grid = new MyGrid<>("Apps", dataProvider);
        grid.setWidth(100, Unit.PERCENTAGE);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.addColumn(App::getName, "Name");
        grid.addColumn(reportRepository::countAllByBugApp, "Reports");
        grid.addItemClickListener(e -> getNavigationManager().navigateTo(AppView.class, String.valueOf(e.getItem().getId())));
        VerticalLayout layout = new VerticalLayout(grid);
        if (SecurityUtils.hasRole(UserManager.ROLE_ADMIN)) {
            Button add = new Button("New App", e -> addApp());
            layout.addComponent(add);
        }
        Style.apply(layout, Style.NO_PADDING, Style.PADDING_LEFT, Style.PADDING_RIGHT, Style.PADDING_BOTTOM);
        setCompositionRoot(layout);
    }

    private void addApp() {
        Window window = new Window("New App");
        TextField name = new TextField("Name");
        Button create = new Button("Create");
        VerticalLayout layout = new VerticalLayout(name, create);
        create.addClickListener(e -> {
            Pair<User, String> userPasswordPair = userManager.createReporterUser();
            appRepository.save(new App(name.getValue(), userPasswordPair.getFirst()));
            grid.getDataProvider().refreshAll();
            layout.removeAllComponents();
            layout.addComponent(new ConfigurationLabel(userPasswordPair.getFirst().getUsername(), userPasswordPair.getSecond()));
            layout.addComponent(new Button("Close", e2 -> window.close()));
            window.center();
        });
        window.setContent(layout);
        window.center();
        UI.getCurrent().addWindow(window);
    }
}