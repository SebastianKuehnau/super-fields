package org.vaadin.miki;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.LoggerFactory;
import org.vaadin.miki.events.text.TextSelectionNotifier;
import org.vaadin.miki.markers.CanReceiveSelectionEventsFromClient;
import org.vaadin.miki.markers.CanSelectText;
import org.vaadin.miki.markers.HasDatePattern;
import org.vaadin.miki.markers.HasLocale;
import org.vaadin.miki.markers.WithNullValueOptionallyAllowed;
import org.vaadin.miki.shared.dates.DatePattern;
import org.vaadin.miki.shared.dates.DatePatterns;
import org.vaadin.miki.superfields.dates.SuperDatePicker;
import org.vaadin.miki.superfields.dates.SuperDateTimePicker;
import org.vaadin.miki.superfields.itemgrid.ItemGrid;
import org.vaadin.miki.superfields.lazyload.ComponentObserver;
import org.vaadin.miki.superfields.lazyload.LazyLoad;
import org.vaadin.miki.superfields.lazyload.ObservedField;
import org.vaadin.miki.superfields.numbers.AbstractSuperNumberField;
import org.vaadin.miki.superfields.numbers.SuperBigDecimalField;
import org.vaadin.miki.superfields.numbers.SuperDoubleField;
import org.vaadin.miki.superfields.numbers.SuperIntegerField;
import org.vaadin.miki.superfields.numbers.SuperLongField;
import org.vaadin.miki.superfields.tabs.SuperTabs;
import org.vaadin.miki.superfields.tabs.TabHandler;
import org.vaadin.miki.superfields.tabs.TabHandlers;
import org.vaadin.miki.superfields.text.SuperTextArea;
import org.vaadin.miki.superfields.text.SuperTextField;
import org.vaadin.miki.superfields.unload.UnloadObserver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Demo app for various SuperFields and other components.
 * @author miki
 * @since 2020-04-07
 */
@CssImport("./styles/demo-styles.css")
@CssImport(value = "./styles/super-number-fields-styles.css", themeFor = "vaadin-text-field")
@CssImport(value = "./styles/super-tabs-styles.css", themeFor = "vaadin-tabs")
@Route
@PageTitle("SuperFields Demo")
public class MainView extends VerticalLayout {

    private static final int NOTIFICATION_TIME = 1500;

    private final Map<Class<?>, Component> components = new LinkedHashMap<>();

    private final Map<Class<?>, SerializableConsumer<Object>> afterLocaleChange = new HashMap<>();

    private final Map<Class<?>, SerializableBiConsumer<Component, Consumer<Component[]>>> contentBuilders = new LinkedHashMap<>();

    private static Component generateParagraph(Class<? extends Component> type, int row, int column) {
        Paragraph result = new Paragraph(type.getSimpleName());
        result.setTitle(String.format("row %d, column %d", row, column));
        return result;
    }

    private static Component generateDiv(Class<? extends Component> type, int row, int column) {
        return new LazyLoad<Div>(() -> {
            final Div result = new Div();
            result.addClassNames("item-grid-cell");
            result.add(new Span(String.format("Row: %d. Column: %d.", row, column)));
            final TextField text = new TextField("Class name: ", type.getSimpleName());
            text.setValue(type.getSimpleName());
            text.addClassName("highlighted");
            text.addBlurListener(event -> text.setValue(type.getSimpleName()));
            result.add(text);
            return result;
        }).withId(type.getSimpleName()+"-"+row+"-"+column);
    }

    private void buildAbstractSuperNumberField(Component component, Consumer<Component[]> callback) {
        final Checkbox autoselect = new Checkbox("Select automatically on focus?");
        autoselect.addValueChangeListener(event -> ((AbstractSuperNumberField<?, ?>) component).setAutoselect(event.getValue()));

        final Checkbox separatorHidden = new Checkbox("Hide grouping separator on focus?");
        separatorHidden.addValueChangeListener(event -> ((AbstractSuperNumberField<?, ?>) component).setGroupingSeparatorHiddenOnFocus(event.getValue()));

        final Checkbox prefix = new Checkbox("Show prefix component?");
        prefix.addValueChangeListener(event -> ((AbstractSuperNumberField<?, ?>) component).setPrefixComponent(
                event.getValue() ? new Span(">") : null
        ));

        final Checkbox suffix = new Checkbox("Show suffix component?");
        suffix.addValueChangeListener(event -> ((AbstractSuperNumberField<?, ?>) component).setSuffixComponent(
                event.getValue() ? new Span("€") : null
        ));

        final Checkbox alignRight = new Checkbox("Align text to the right?");
        alignRight.addValueChangeListener(event -> {
                    if(event.getValue())
                        ((AbstractSuperNumberField<?, ?>) component).addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
                    else
                        ((AbstractSuperNumberField<?, ?>) component).removeThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
                }
        );
        callback.accept(new Component[]{autoselect, separatorHidden, prefix, suffix, alignRight});
    }

    private void buildFocusNotifier(Component component, Consumer<Component[]> callback) {
        ((FocusNotifier<?>)component).addFocusListener(event ->
           Notification.show("Component "+component.getClass().getSimpleName()+" received focus.", NOTIFICATION_TIME, Notification.Position.BOTTOM_END)
        );
        callback.accept(new Component[]{new Span("Focus the demo component to see a notification.")});
    }

    private void buildBlurNotifier(Component component, Consumer<Component[]> callback) {
        ((BlurNotifier<?>)component).addBlurListener(event ->
                Notification.show("Component "+component.getClass().getSimpleName()+" lost focus.", NOTIFICATION_TIME, Notification.Position.BOTTOM_END)
        );
        callback.accept(new Component[]{new Span("Leave the demo component to see a notification.")});
    }

    private void buildHasLocale(Component component, Consumer<Component[]> callback) {
        final ComboBox<Locale> locales = new ComboBox<>("Select locale:", new Locale("pl", "PL"), Locale.UK, Locale.FRANCE, Locale.GERMANY, Locale.CHINA);
        locales.setItemLabelGenerator(locale -> locale.getDisplayCountry() + " / "+locale.getDisplayLanguage());
        locales.setAllowCustomValue(false);
        locales.addValueChangeListener(event -> {
            ((HasLocale) component).setLocale(event.getValue());
            if(this.afterLocaleChange.containsKey(component.getClass()))
                this.afterLocaleChange.get(component.getClass()).accept(component);
        });
        callback.accept(new Component[]{locales});
    }

    private void buildNullValueOptionallyAllowed(Component component, Consumer<Component[]> callback) {
        final Checkbox allow = new Checkbox("Allow empty string as null value?", event -> ((WithNullValueOptionallyAllowed<?, ?, ?>)component).setNullValueAllowed(event.getValue()));
        callback.accept(new Component[]{allow});
    }

    private void buildHasValue(Component component, Consumer<Component[]> callback) {
        final Checkbox toggle = new Checkbox("Mark component as read only?", event -> ((HasValue<?, ?>)component).setReadOnly(event.getValue()));
        ((HasValue<?, ?>) component).addValueChangeListener(this::onAnyValueChanged);
        callback.accept(new Component[]{toggle});
    }

    private void buildCanSelectText(Component component, Consumer<Component[]> callback) {
        final Button selectAll = new Button("Select all", event -> ((CanSelectText)component).selectAll());
        final Button selectNone = new Button("Select none", event -> ((CanSelectText)component).selectNone());
        final HorizontalLayout layout = new HorizontalLayout(new Span("Type something in the field, then click one of the buttons:"), selectAll, selectNone);
        layout.setAlignItems(Alignment.CENTER);
        callback.accept(new Component[]{
                layout
        });
        if(component instanceof CanReceiveSelectionEventsFromClient) {
            final Checkbox receiveFromClient = new Checkbox("Allow selection events initiated by keyboard or mouse?",
                    event -> ((CanReceiveSelectionEventsFromClient) component).setReceivingSelectionEventsFromClient(event.getValue()));
            callback.accept(new Component[] {receiveFromClient});
        }
        if(component instanceof TextSelectionNotifier<?>) {
            final Span selection = new Span();
            ((TextSelectionNotifier<?>) component).addTextSelectionListener(event -> selection.setText(event.getSelectedText()));
            Icon icon = VaadinIcon.INFO_CIRCLE.create();
            icon.setColor("green");
            icon.getElement().setAttribute("title", "When the component does not receive events from the browser, selection events will only be called for server-side initiated actions.");
            callback.accept(new Component[]{
                    new HorizontalLayout(new Span("Most recently selected text: <"), selection, new Span(">"), icon)
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void buildItemGrid(Component component, Consumer<Component[]> callback) {
        final RadioButtonGroup<Integer> buttons = new RadioButtonGroup<>();
        buttons.setItems(1, 2, 3, 4, 5, 6);
        buttons.setLabel("Number of columns:");
        buttons.setValue(ItemGrid.DEFAULT_COLUMN_COUNT);
        buttons.addValueChangeListener(event -> ((ItemGrid<?>)component).setColumnCount(event.getValue()));

        final Checkbox alternate = new Checkbox("Display lazy loading cells?", event ->
                ((ItemGrid<Class<? extends Component>>)component).setCellGenerator(
                        event.getValue() ? MainView::generateDiv : MainView::generateParagraph
                )
        );

        callback.accept(new Component[]{buttons, alternate});
    }

    private void buildHasDatePattern(Component component, Consumer<Component[]> callback) {
        final ComboBox<DatePattern> patterns = new ComboBox<>("Select date display pattern:", DatePatterns.YYYY_MM_DD, DatePatterns.M_D_YYYY_SLASH, DatePatterns.DD_MM_YYYY_DOTTED, DatePatterns.D_M_YY_DOTTED);
        final Button clearPattern = new Button("Clear pattern", event -> ((HasDatePattern)component).setDatePattern(null));
        clearPattern.setDisableOnClick(true);
        final Component clearPatternOrContainer;
        // issue #87 requires a note
        if(component instanceof SuperDatePicker)
            clearPatternOrContainer = clearPattern;
        else {
            Icon icon = new Icon(VaadinIcon.INFO);
            icon.setColor("green");
            icon.getElement().setAttribute("title", "Setting pattern does not work out of the box for SuperDateTimePicker if it is in an invisible layout. See issue #87, https://github.com/vaadin-miki/super-fields/issues/87.");
            clearPatternOrContainer = new HorizontalLayout(clearPattern, icon);
            ((HorizontalLayout)clearPatternOrContainer).setAlignItems(Alignment.CENTER);
        }
        patterns.addValueChangeListener(event -> {
            ((HasDatePattern) component).setDatePattern(event.getValue());
            clearPattern.setEnabled(true);
        });
        callback.accept(new Component[]{patterns, clearPatternOrContainer});
    }

    private void buildObservedField(Component component, Consumer<Component[]> callback) {
        final Span description = new Span("An instance of ObservedField is added below these texts. It has a value change listener that updates the counter whenever the field is shown on screen, for example as a result of resizing window or scrolling. The value does not change when the field gets hidden due to styling (display: none). The field itself in an empty HTML and it cannot be normally seen, but it still is rendered by the browser.");
        final Span counterText = new Span("The field has become visible this many times so far: ");
        final Span counterLabel = new Span("0");
        final Span instruction = new Span("The field is rendered below this text. Resize the window a few times to hide this line to see the value change events being triggered.");
        counterLabel.addClassName("counter-label");
        ((ObservedField)component).addValueChangeListener(event -> {
            if(event.getValue())
                counterLabel.setText(String.valueOf(Integer.parseInt(counterLabel.getText()) + 1));
        });
        callback.accept(new Component[]{description, new HorizontalLayout(counterText, counterLabel), instruction});
    }

    private void buildIntersectionObserver(Component component, Consumer<Component[]> callback) {
        for(String s: new String[]{"span-one", "span-two", "span-three"}) {
            Span span = new Span("This text is observed by the intersection observer. Resize the window to make it disappear and see what happens. It has id of "+s+". ");
            span.setId(s);
            ((ComponentObserver)component).observe(span);
            callback.accept(new Component[]{span});
        }
        ((ComponentObserver) component).addComponentObservationListener(event -> {
            if(event.isFullyVisible()) {
                Notification.show("Component with id " + event.getObservedComponent().getId().orElse("(no id)") + " is now fully visible.");
                if(event.getObservedComponent().getId().orElse("").equals("span-two")) {
                    event.getSource().unobserve(event.getObservedComponent());
                    Notification.show("Component with id span-two has been unobserved.");
                }
            }
            else if(event.isNotVisible())
                Notification.show("Component with id "+event.getObservedComponent().getId().orElse("(no id)")+" is now not visible.");
        });
    }

    private void buildUnloadObserver(Component component, Consumer<Component[]> callback) {
        final Checkbox query = new Checkbox("Query on window unload?", event -> ((UnloadObserver)component).setQueryingOnUnload(event.getValue()));
        final Span description = new Span("This component optionally displays a browser-native window when leaving this app. Select the checkbox above and try to close the window or tab to see it in action.");
        final Span counterText = new Span("There were this many attempts to leave this app so far: ");
        final Span counter = new Span("0");
        ((UnloadObserver)component).addUnloadListener(event -> {
            if(event.isBecauseOfQuerying())
                counter.setText(String.valueOf(Integer.parseInt(counter.getText()) + 1));
            LoggerFactory.getLogger(this.getClass()).info("Unload event; attempt? {}; captured in {} and UnloadObserver is inside {}", event.isBecauseOfQuerying(), this.getClass().getSimpleName(), event.getSource().getParent().orElse(this).getClass().getSimpleName());
        });

        callback.accept(new Component[]{query, description, new HorizontalLayout(counterText, counter)});
    }

    private void buildSuperTabs(Component component, Consumer<Component[]> callback) {
        final Checkbox multilineTabs = new Checkbox("Multiline tabs?", event -> ((SuperTabs<?>)component).setMultiline(event.getValue()));

        final ComboBox<TabHandler> tabHandlers = new ComboBox<>("Select a tab handler: ",
                TabHandlers.VISIBILITY_HANDLER, TabHandlers.REMOVING_HANDLER, TabHandlers.selectedContentHasClassName("selected-tab"));
        tabHandlers.addValueChangeListener(event -> {
            if(event.getValue() != null)
                ((SuperTabs<?>)component).setTabHandler(event.getValue());
        });

        callback.accept(new Component[]{multilineTabs, tabHandlers});
    }

    private Component buildContentsFor(Class<?> type) {
        Component component = this.components.get(type);
        component.getElement().getClassList().add("demo");
        Div result = new Div();
        result.setSizeUndefined();
        result.addClassName("component-page");
        VerticalLayout componentSection = new VerticalLayout();
        componentSection.setSizeUndefined();
        componentSection.addClassName("component-section");
        Span title = new Span("Demo page of "+component.getClass().getSimpleName());
        title.addClassName("section-header");
        title.addClassName("component-header");
        componentSection.add(title, component);
        result.add(componentSection);

        this.contentBuilders.entrySet().stream().
                filter(entry -> entry.getKey().isAssignableFrom(type)).
                forEach(entry -> {
                    VerticalLayout section = new VerticalLayout();
                    section.setSizeUndefined();
                    section.addClassName("section-layout");
                    Span header = new Span("Configuration options for "+entry.getKey().getSimpleName());
                    header.addClassName("section-header");
                    section.add(header);
                    entry.getValue().accept(component, section::add);
                    result.add(section);
                });
        return result;
    }

    private void onAnyValueChanged(HasValue.ValueChangeEvent<?> valueChangeEvent) {
        Notification.show(String.format("%s changed value to %s", valueChangeEvent.getHasValue().getClass().getSimpleName(), valueChangeEvent.getValue()));
    }

    public MainView() {
        this.components.put(SuperIntegerField.class, new SuperIntegerField("Integer (6 digits):").withMaximumIntegerDigits(6));
        this.components.put(SuperLongField.class, new SuperLongField("Long (11 digits):").withMaximumIntegerDigits(11).withId("long"));
        this.components.put(SuperDoubleField.class, new SuperDoubleField("Double (8 + 4 digits):").withMaximumIntegerDigits(8).withMaximumFractionDigits(4));
        this.components.put(SuperBigDecimalField.class, new SuperBigDecimalField("Big decimal (12 + 3 digits):").withMaximumIntegerDigits(12).withMaximumFractionDigits(3).withMinimumFractionDigits(1).withId("big-decimal"));
        this.components.put(SuperDatePicker.class, new SuperDatePicker("Pick a date:").withDatePattern(DatePatterns.YYYY_MM_DD).withValue(LocalDate.now()));
        this.components.put(SuperDateTimePicker.class, new SuperDateTimePicker("Pick a date and time:").withDatePattern(DatePatterns.M_D_YYYY_SLASH).withValue(LocalDateTime.now()));
        this.components.put(SuperTextField.class, new SuperTextField("Type something:").withPlaceholder("(nothing typed)").withId("super-text-field"));
        this.components.put(SuperTextArea.class, new SuperTextArea("Type a lot of something:").withPlaceholder("(nothing typed)").withId("super-text-area"));
        this.components.put(SuperTabs.class, new SuperTabs<String>((Supplier<HorizontalLayout>) HorizontalLayout::new)
                .withTabContentGenerator(s -> new Paragraph("Did you know? All SuperFields are "+s))
                .withItems(
                        "Java friendly", "Super-configurable", "Open source",
                        "Fun to use", "Reasonably well documented"
                ).withId("super-tabs")
        );
        this.components.put(ObservedField.class, new ObservedField());
        this.components.put(ComponentObserver.class, new ComponentObserver());
        this.components.put(UnloadObserver.class, UnloadObserver.get(false));
        this.components.put(ItemGrid.class, new ItemGrid<Class<? extends Component>>(
                null,
                () -> {
                    VerticalLayout result = new VerticalLayout();
                    result.setSpacing(true);
                    result.setPadding(true);
                    result.setAlignItems(Alignment.STRETCH);
                    result.setWidthFull();
                    return result;
                },
                MainView::generateParagraph,
                event -> {
                    if (event.isSelected())
                        event.getCellInformation().getComponent().getElement().getClassList().add("selected");
                    else event.getCellInformation().getComponent().getElement().getClassList().remove("selected");
                },
                SuperIntegerField.class, SuperLongField.class, SuperDoubleField.class,
                SuperBigDecimalField.class, SuperDatePicker.class, SuperDateTimePicker.class,
                SuperTabs.class, LazyLoad.class, ObservedField.class,
                ComponentObserver.class, UnloadObserver.class, ItemGrid.class
                )
            .withRowComponentGenerator(rowNumber -> {
                    HorizontalLayout result = new HorizontalLayout();
                    result.setSpacing(true);
                    result.setAlignItems(Alignment.CENTER);
                    result.setPadding(true);
                    return result;
                })
        );

        this.contentBuilders.put(CanSelectText.class, this::buildCanSelectText);
        this.contentBuilders.put(HasValue.class, this::buildHasValue);
        this.contentBuilders.put(AbstractSuperNumberField.class, this::buildAbstractSuperNumberField);
        this.contentBuilders.put(WithNullValueOptionallyAllowed.class, this::buildNullValueOptionallyAllowed);
        this.contentBuilders.put(HasLocale.class, this::buildHasLocale);
        this.contentBuilders.put(ItemGrid.class, this::buildItemGrid);
        this.contentBuilders.put(HasDatePattern.class, this::buildHasDatePattern);
        this.contentBuilders.put(SuperTabs.class, this::buildSuperTabs);
        this.contentBuilders.put(ObservedField.class, this::buildObservedField);
        this.contentBuilders.put(ComponentObserver.class, this::buildIntersectionObserver);
        this.contentBuilders.put(UnloadObserver.class, this::buildUnloadObserver);
        this.contentBuilders.put(FocusNotifier.class, this::buildFocusNotifier);
        this.contentBuilders.put(BlurNotifier.class, this::buildBlurNotifier);

        this.afterLocaleChange.put(SuperIntegerField.class, o -> ((SuperIntegerField)o).setMaximumIntegerDigits(6));
        this.afterLocaleChange.put(SuperLongField.class, o -> ((SuperLongField)o).setMaximumIntegerDigits(11));
        this.afterLocaleChange.put(SuperDoubleField.class, o -> ((SuperDoubleField)o).withMaximumIntegerDigits(8).setMaximumFractionDigits(4));
        this.afterLocaleChange.put(SuperBigDecimalField.class, o -> ((SuperBigDecimalField)o).withMaximumIntegerDigits(12).withMaximumFractionDigits(3).setMinimumFractionDigits(1));

        final SuperTabs<Class<?>> tabs = new SuperTabs<Class<?>>(
                type -> new Tab(type.getSimpleName()),
                this::buildContentsFor
        );

        tabs.addTab(MainView.class, new Tab(new Icon(VaadinIcon.INFO_CIRCLE), new Span("SuperFields demo")), new InfoPage());

        tabs.addTab(this.components.keySet().toArray(new Class<?>[0]));

        this.add(tabs);
    }
}
