package vciptvman.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.atmosphere.interceptor.AtmosphereResourceStateRecovery;
import vciptvman.database.BookmarkDatabase;
import vciptvman.database.EpgStreamDatabase;
import vciptvman.model.*;

import java.util.*;
import java.util.function.Consumer;

public class StreamComponent extends VerticalLayout implements EpgdBufferCreator {

    private static EpgStreamDatabase epgstream;
    private static BookmarkDatabase bookmarks;

    private Grid<Stream> streamsGrid;
    private Map<String, String> categories;
    private Map<String, VdrChannel> vdrChannels = new HashMap<>();
    private List<VdrChannel> vdrChannelNames = new ArrayList<>();

    private Stream draggedItem;
    StreamFilter streamFilter;

    List<Stream> streams;

    private boolean showOnlyBookmarked;

    private Grid.Column<Stream> nameColumn;
    private Grid.Column<Stream> countryColumn;
    private Grid.Column<Stream> categoryColumn;
    private Grid.Column<Stream> epgColumn;
    private Grid.Column<Stream> vdrColumn;

    public StreamComponent(boolean bookmarked) {
        showOnlyBookmarked = bookmarked;

        epgstream = new EpgStreamDatabase();
        bookmarks = new BookmarkDatabase();

        categories = epgstream.getAllCategories();

        List<VdrChannel> channels = bookmarks.getVdrChannels();
        channels.stream().forEach(channel -> {
            vdrChannels.put(channel.xmltv_id(), channel);
            vdrChannelNames.add(channel);
        });

        createLayout();
    }

    private void createLayout() {
        assert epgstream != null;

        if (showOnlyBookmarked) {
            streamsGrid = createStreamsGrid(epgstream.getBookmarkedStreams());
        } else {
            streamsGrid = createStreamsGrid(epgstream.getAllStreams());
        }

        setSizeFull();
        setMargin(false);

        FlexLayout flex = new FlexLayout();
        flex.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        FlexLayout actions = new FlexLayout();
        actions.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        Button exportStream = new Button("Export stream list");
        exportStream.addClickListener(e -> exportStream());
        actions.add(exportStream);

        Button exportEpg = new Button("Export EPG channel list");
        exportEpg.addClickListener(e -> exportEpg());
        actions.add(exportEpg);

        Button exportEpgd = new Button("Export epgd channelmap.conf fragment");
        exportEpgd.addClickListener(e -> exportEpgd());
        actions.add(exportEpgd);

        add(actions);

        add(streamsGrid);
    }

    private void exportEpg() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <channels>
                """);

        List<Bookmark> b = bookmarks.getAllBookmarks();
        b.stream().forEach(bookmark -> {
            if (bookmark.site() != null && !bookmark.site().isEmpty()) {
                String siteId = epgstream.getSiteId(bookmark.xmltv_id(), bookmark.site(), bookmark.site_lang());

                if (StringUtils.isNotEmpty(siteId)) {
                    buffer.append("    <channel site=\"" + bookmark.site() + "\" lang=\"" + bookmark.site_lang() + "\" xmltv_id=\"" + bookmark.xmltv_id() + "\" site_id=\"" + siteId + "\">" + StringEscapeUtils.escapeXml10(epgstream.getName(bookmark.xmltv_id())) + "</channel>\n");
                } else {
                    buffer.append("<!-- Dieser Kanal kann nicht gefunden werden. Wahrscheinlich stimmt die xmltv_id nicht.\n");
                    buffer.append("    <channel site=\"" + bookmark.site() + "\" lang=\"" + bookmark.site_lang() + "\" xmltv_id=\"" + bookmark.xmltv_id() + "\" site_id=\"" + siteId + "\">" + StringEscapeUtils.escapeXml10(epgstream.getName(bookmark.xmltv_id())) + "</channel>\n");
                    buffer.append("-->\n");
                }
            }
        });

        buffer.append("</channels>\n");

        String saveTo = System.getProperty("epg-channels.save-to");
        ExportDialog a = new ExportDialog("EPG", buffer.toString(), saveTo, ExportDialog.ExportType.EPG_CHANNELLIST, null);
        a.open();
    }

    private void exportStream() {
        StringBuffer buffer = new StringBuffer();

        List<Bookmark> b = bookmarks.getAllBookmarks();
        b.stream().forEach(bookmark -> {
           if (bookmark.url() != null && !bookmark.url().isEmpty()) {
               buffer.append(epgstream.getName(bookmark.xmltv_id()) + ";" + bookmark.url() + "\n");
           }
        });

        String saveTo = System.getProperty("stream-channels.save-to");
        ExportDialog a = new ExportDialog("Streams", buffer.toString(), saveTo, ExportDialog.ExportType.STREAMS, null);
        a.open();
    }

    public StringBuffer createExportEpgdBuffer(boolean addVdr, boolean addAllOthers) {
        StringBuffer buffer = new StringBuffer();

        List<Bookmark> book = bookmarks.getAllBookmarks().stream().filter(b -> b.site() != null && !b.site().isEmpty() && b.active()).toList();
        List<VdrChannel> chan = bookmarks.getVdrChannels().stream().filter(b -> b.other_id() != null || b.xmltv_id() != null).toList();
        List<VdrChannel> chan2 = bookmarks.getVdrChannels().stream().filter(b -> b.other_id() == null && b.xmltv_id() == null).toList();
        List<OtherEpgProvider> op = epgstream.getOtherEpgProvider();

        // at first all VdrChannel
        chan.stream().forEach(c -> {
            boolean added = false;

            buffer.append("// " + c.name() + "\n");

            if (addVdr) {
                buffer.append("vdr:000:0 = " + c.channel_id() + "\n");
            }

            if (c.xmltv_id() != null) {
                buffer.append("xmltv:" +  c.xmltv_id() + ":1 = " + c.channel_id() + "\n");
                added = true;
            }

            if (c.other_id() != null) {
                buffer.append("other:" +  op.stream().filter(e -> e.name().equals(c.other_id())).findFirst().get().id() + (added ? ":2" : ":1") + " = " + c.channel_id() + "\n");
            }

            buffer.append("\n");
        });

        // all Bookmarks which does not exists in VdrChannel
        book.stream().forEach(bookmark -> {
            boolean exists = chan.stream().filter(x -> x.xmltv_id() != null && x.xmltv_id().equals(bookmark.xmltv_id())).findFirst().isPresent();
            if (!exists) {
                buffer.append("// " + epgstream.getName(bookmark.xmltv_id()) + "\n");

                if (addVdr) {
                    buffer.append("// vdr:000:0 = \n");
                }

                buffer.append("// xmltv:" + bookmark.xmltv_id() + ":1 = \n\n");
            }
        });

        // all channels without a configured epg provider
        if (addAllOthers) {
            chan2.stream().forEach(c -> {
                buffer.append("// " + c.name() + "\n");
                buffer.append("vdr:0000:0 = " + c.channel_id() + "\n\n");
            });
        }

        return buffer;
    }

    private void exportEpgd() {
        StringBuffer buffer = createExportEpgdBuffer(false, false);

        String saveTo = System.getProperty("epgd-channelmap.save-to");
        ExportDialog a = new ExportDialog("epgd channelmap.conf", buffer.toString(), saveTo, ExportDialog.ExportType.EPGD_CHANNELMAP, this);
        a.open();
    }

    private Grid<Stream> createStreamsGrid(List<Stream> streams) {
        Grid<Stream> grid = new Grid<>(Stream.class, false);

        this.streams = streams;

        grid.addColumn(createBookmarkRenderer(grid)).setWidth("50px").setFlexGrow(0).setFrozen(true);
        grid.addColumn(createToggleDetailsRenderer(grid)).setWidth("70px").setFlexGrow(0).setFrozen(true);

        Comparator<Stream> logoNameComp = new Comparator<Stream>() {
            @Override
            public int compare(Stream o1, Stream o2) {
                return o1.name().compareToIgnoreCase(o2.name());
            }
        };

        nameColumn = grid.addColumn(createLogoRenderer()).setHeader("Name").setSortable(true).setComparator(logoNameComp);
        vdrColumn = grid.addColumn(createVdrRenderer()).setHeader("VDR").setSortable(false);
        countryColumn = grid.addColumn(createCountryColumnRenderer()).setHeader("Country").setSortable(true).setComparator(Stream::country);
        categoryColumn = grid.addColumn(createCategoryRenderer()).setHeader("Category").setSortable(true).setComparator(Stream::categories);
        epgColumn = grid.addColumn(createEpgProviderRenderer()).setHeader("EPG Provider").setSortable(false);

        if (showOnlyBookmarked) {
            grid.addColumn(createDeleteButton());
        }

        grid.setItems(streams);
        grid.setMultiSort(true, Grid.MultiSortPriority.APPEND);
        grid.setItemDetailsRenderer(createStreamDetailsRenderer());

        streamFilter = new StreamFilter(grid.getListDataView());

        addFilterHeader(streams, grid);

        if (showOnlyBookmarked) {
            grid.setRowsDraggable(true);

            grid.addDragStartListener(e -> {
                draggedItem = e.getDraggedItems().get(0);
                grid.setDropMode(GridDropMode.BETWEEN);
            });

            grid.addDropListener(e -> {
                Stream targetStream = e.getDropTargetItem().orElse(null);
                GridDropLocation dropLocation = e.getDropLocation();

                boolean personWasDroppedOntoItself = draggedItem.equals(targetStream);

                if (targetStream == null || personWasDroppedOntoItself)
                    return;

                streamsGrid.getListDataView().removeItem(draggedItem);

                if (dropLocation == GridDropLocation.BELOW) {
                    streamsGrid.getListDataView().addItemAfter(draggedItem, targetStream);
                } else {
                    streamsGrid.getListDataView().addItemBefore(draggedItem, targetStream);
                }
            });

            grid.addDragEndListener(e -> {
                for (int i = 0; i < streamsGrid.getListDataView().getItemCount(); ++i) {
                    Stream s = streamsGrid.getListDataView().getItem(i);
                    bookmarks.updateSortOrderBookmark(new Bookmark(s.xmltv_id(), null, null, null, false, i+1));
                }

                draggedItem = null;
                grid.setDropMode(null);
            });
        }

        return grid;
    }

    private void addFilterHeader(List<Stream> streams, Grid<Stream> grid) {
        grid.removeAllHeaderRows();

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(nameColumn).setComponent(createNameFilterHeader(e -> streamFilter.setName(e)));
        headerRow.getCell(countryColumn).setComponent(createCountryFilterHeader(getAvailableCountries(streams), e -> streamFilter.setCountry(e)));
        headerRow.getCell(categoryColumn).setComponent(createCategoryFilterHeader(e -> streamFilter.setCategory(e)));

        headerRow.getCell(vdrColumn).setComponent(new Div("VDR channel"));
        headerRow.getCell(epgColumn).setComponent(new Div("EPG Provider"));

        if (!showOnlyBookmarked) {
            headerRow.getCell(epgColumn).setComponent(createEpgSiteFilterHeader());
        }
    }

    private List<Country> getAvailableCountries(List<Stream> streams) {
        List<Country> countries = new ArrayList<>();

        streams.stream().map(Stream::country_code).distinct().forEach(s -> {
            if (s != null) {
                countries.add(epgstream.getCountry(s));
            }
        });

        countries.sort((o1, o2) -> {
            return o1.name().compareToIgnoreCase(o2.name());
        });

        return countries;
    }

    private Renderer<Stream> createToggleDetailsRenderer(Grid<Stream> grid) {
        return LitRenderer
                .<Stream> of("""                                
                <vaadin-button
                    theme="tertiary icon"
                    aria-label="Toggle details"
                    aria-expanded="${model.detailsOpened ? 'true' : 'false'}"
                    @click="${handleClick}">
                        <vaadin-icon .icon="${model.detailsOpened ? 'lumo:angle-down' : 'lumo:angle-right'}"
                    ></vaadin-icon>
                </vaadin-button>
            """)
                .withFunction("handleClick",
                        stream -> grid.setDetailsVisible(stream, !grid.isDetailsVisible(stream)));
    }

    private Renderer<Stream> createBookmarkRenderer(Grid<Stream> grid) {
        return new ComponentRenderer<>(stream -> {
            Checkbox checkbox = new Checkbox();
            checkbox.addValueChangeListener(b -> {
                bookmarks.updateEnabledBookmark(new Bookmark(stream.xmltv_id(), null, null, null, b.getValue(), 0));
            });

            checkbox.setValue(bookmarks.getActive(stream.xmltv_id()));

            return checkbox;
        });
    }

    private Renderer<Stream> createEpgProviderRenderer() {
        return new ComponentRenderer<>(stream -> {
            List<EpgSite> providers = epgstream.getEpgProviderById(stream.xmltv_id());

            HorizontalLayout hLayout = new HorizontalLayout();

            if (!providers.isEmpty()) {
                ComboBox<EpgSite> cbProvider = new ComboBox<>();
                cbProvider.setItems(providers);
                cbProvider.setRenderer(createEpgSiteRenderer());
                cbProvider.setItemLabelGenerator(site -> site.site() != null ? site.site() + " (" + site.site_lang() + ")" : "");

                cbProvider.addValueChangeListener(b -> {
                    bookmarks.updateSiteBookmark(new Bookmark(stream.xmltv_id(), b.getValue() != null ? b.getValue().site() : null, b.getValue() != null ? b.getValue().site_lang() : null, null, false, 0));
                    bookmarks.setVdrChannel(stream, null, null);
                });

                cbProvider.setValue(bookmarks.getSite(stream.xmltv_id()));

                hLayout.add(cbProvider);
            } else {
                Div cbProvider = new Div("");
                hLayout.add(cbProvider);
            }

            return hLayout;
        });
    }

    private Renderer<Stream> createStreamDetailsRenderer() {
        return new ComponentRenderer<>(stream -> {
            Button homepage = new Button("Channel Homepage", event -> {
                UI.getCurrent().getPage().open(stream.website(),"_blank");
            });

            Button epgwebsite = new Button("EPG Site", event -> {
                UI.getCurrent().getPage().open("https://" + bookmarks.getSite(stream.xmltv_id()).site(), "_blank");
            });

            TextField xmltvId = new TextField();
            xmltvId.setValue(stream.xmltv_id());
            xmltvId.setWidth("50em");

            if (stream.website() == null) {
                homepage.setEnabled(false);
            }

            if (bookmarks.getSite(stream.xmltv_id()) == null) {
                epgwebsite.setEnabled(false);
            }

            Grid<StreamUrl> grid = null;
            List<StreamUrl> urlList = epgstream.getStreamUrls(stream.xmltv_id());
            if (!urlList.isEmpty()) {
                grid = new Grid<>(StreamUrl.class, false);

                grid.addColumn(createVideoPreviewRenderer()).setSortable(false).setAutoWidth(true).setFlexGrow(0);
                grid.addColumn(StreamUrl::name).setHeader("Name").setSortable(false).setAutoWidth(true).setFlexGrow(0);
                grid.addColumn(StreamUrl::url).setHeader("URL").setSortable(false).setAutoWidth(true).setFlexGrow(1);
                grid.setItems(urlList);
                grid.setAllRowsVisible(true);

                String bookmarkedUrl = bookmarks.getUrl(stream.xmltv_id());
                urlList.stream().filter(url -> StringUtils.isNotEmpty(url.url()) && url.url().equals(bookmarkedUrl)).findFirst().ifPresent(grid::select);

                grid.addSelectionListener(event -> {
                    Optional val = event.getFirstSelectedItem();
                    if (val.isPresent()) {
                        StreamUrl url = (StreamUrl) val.get();
                        bookmarks.updateUrlBookmark(new Bookmark(stream.xmltv_id(), null, null, url.url(), false, 0));
                    } else {
                        bookmarks.updateUrlBookmark(new Bookmark(stream.xmltv_id(), null, null, null, false, 0));
                    }
                });
            }

            VerticalLayout vLayout = new VerticalLayout();
            HorizontalLayout hLayout = new HorizontalLayout(homepage, epgwebsite, xmltvId);

            vLayout.add(hLayout);

            if (grid != null) {
                vLayout.add(grid);
            }

            return vLayout;
        });
    }

    private Renderer<StreamUrl> createVideoPreviewRenderer() {
        return new ComponentRenderer<>(stream -> {
            Button preview = new Button("Preview Stream", event -> {
                VideoDialog a = new VideoDialog(stream.name(), stream.url());
                a.open();
            });

            return preview;
        });
    }

    private Renderer<Stream> createLogoRenderer() {
        return LitRenderer.<Stream> of("""
                        <vaadin-vertical-layout>
                            <image src="${item.logo}" style="max-height:30px"></image>
                            ${item.name}<br/>
                            <span style="font-size: smaller;color:grey;">${item.xmltvid}</span>
                        </vaadin-vertical-layout>
                        """)
                .withProperty("logo", Stream::logo)
                .withProperty("name", Stream::name)
                .withProperty("xmltvid", Stream::xmltv_id);
    }

    private Renderer<Stream> createVdrRenderer() {
        return new ComponentRenderer<>(stream -> {
            ComboBox<VdrChannel> vdrs =  new ComboBox<>();
            vdrs.setItems(vdrChannelNames);
            vdrs.setRenderer(createvdrNameRenderer());
            vdrs.setItemLabelGenerator(ch -> ch != null ? ch.name() : "");
            vdrs.setValue(vdrChannels.get(stream.xmltv_id()));

            vdrs.addValueChangeListener(event -> {
                bookmarks.setVdrChannel(stream, event.getValue(), event.getOldValue());
            });

            return vdrs;
        });
    }

    private Renderer<VdrChannel> createvdrNameRenderer() {
        return LitRenderer.<VdrChannel> of("""
                        <div style="display: flex;">
                            ${item.name}
                        </div>
                        """)
                .withProperty("name", VdrChannel::name);
    }

    private Renderer<Stream> createCountryColumnRenderer() {
        return LitRenderer.<Stream> of("""
                        <div style="display: flex;">
                            <div>
                                ${item.flag} ${item.name}
                            </div>
                        </div>
                        """)
                .withProperty("flag", Stream::flag)
                .withProperty("name", Stream::country);
    }

    private Renderer<Country> createCountryRenderer() {
        String tpl = """
             <div style="display: flex;">
                  <div>
                      ${item.flag} ${item.name}
                  </div>
            </div>
            """;

        return LitRenderer.<Country> of(tpl)
                .withProperty("flag", Country::flag)
                .withProperty("name", Country::name);
    }

    private Renderer<Stream> createCategoryRenderer() {
        return new ComponentRenderer<>(stream -> {
            String toCheck;
            if (stream.categories() == null || stream.categories().isEmpty()) {
                toCheck = "";
            } else {
                toCheck = stream.categories();
            }

            String[] c = Arrays.stream(toCheck.split(";")).sorted().toArray(String[]::new);

            HorizontalLayout hLayout = new HorizontalLayout();
            hLayout.setWrap(true);
            for (String s : c) {
                hLayout.add(new Div(categories.get(s)));
            }
            return hLayout;
        });
    }

    private Renderer<Stream> createDeleteButton() {
        return new ComponentRenderer<>(stream -> {
            Button delete = new Button(new Icon(VaadinIcon.TRASH), event -> {
                bookmarks.deleteBookmark(stream.xmltv_id());
                streamsGrid.setItems(epgstream.getBookmarkedStreams());
            });

            return delete;
        });
    }

    private Component createNameFilterHeader(Consumer<String> filterChangeConsumer) {
        TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.getStyle().set("max-width", "100%");
        textField.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));

        return textField;
    }

    private Component createCountryFilterHeader(List<Country> countries, Consumer<String> filterChangeConsumer) {
        ComboBox<Country> comboBox = new ComboBox<>();
        comboBox.setItems(countries);
        comboBox.setItemLabelGenerator(country -> country.flag() + " " + country.name());
        comboBox.setRenderer(createCountryRenderer());
        comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        comboBox.setRequired(false);
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue() != null ? e.getValue().name() : null));

        if (!showOnlyBookmarked) {
            comboBox.setValue(epgstream.getCountry("DE"));
        }

        return comboBox;
    }

    private Component createCategoryFilterHeader(Consumer<String> filterChangeConsumer) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(categories.values().stream().sorted().toList());
        comboBox.setItemLabelGenerator(item -> item);
        comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        comboBox.setRequired(false);
        comboBox.addValueChangeListener(e -> filterChangeConsumer.accept(e.getValue()));

        return comboBox;
    }

    private Component createEpgSiteFilterHeader() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(epgstream.getAllEpgSites().stream().sorted().toList());
        comboBox.setItemLabelGenerator(item -> item);
        comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
        comboBox.setRequired(false);
        comboBox.addValueChangeListener(e ->  {
            if (StringUtils.isNotEmpty(e.getValue())) {
                streams = epgstream.getAllStreamsWithSite(e.getValue());
            } else {
                streams = epgstream.getAllStreams();
            }

            ListDataProvider dp = (ListDataProvider) streamsGrid.getDataProvider();
            List<Stream> dpList = (List<Stream>) dp.getItems();

            dpList.clear();
            dpList.addAll(streams);

            streamFilter.setDataView(streamsGrid.getListDataView());
            streamsGrid.getDataProvider().refreshAll();
        });

        return comboBox;
    }


    private Renderer<EpgSite> createEpgSiteRenderer() {
        return LitRenderer.<EpgSite> of("""
                        <div style="display: flex;">
                            <div>
                                ${item.site} (${item.site_lang})
                            </div>
                        </div>
                        """)
                .withProperty("site", EpgSite::site)
                .withProperty("site_lang", EpgSite::site_lang);
    }

    private static class StreamFilter {
        private GridListDataView<Stream> dataView;

        private String name;
        private String country;
        private String category;

        public StreamFilter(GridListDataView<Stream> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setDataView(GridListDataView<Stream> dataView) {
            this.dataView = dataView;
            this.dataView.refreshAll();
        }

        public void setName(String name) {
            this.name = name;
            this.dataView.refreshAll();
        }

        public void setCountry(String country) {
            this.country = country;
            this.dataView.refreshAll();
        }

        public void setCategory(String category) {
            this.category = category;
            this.dataView.refreshAll();
        }

        public boolean test(Stream stream) {
            boolean matchesName = matches(stream.name(), name);
            boolean matchesCountry = matches(stream.country(), country);
            boolean matchesCategory = matches(stream.categories(), category);

            return matchesName && matchesCountry && matchesCategory;
        }

        private boolean matches(String value, String searchTerm) {
            if (searchTerm != null && value == null) {
                return false;
            }

            return (value == null) || (searchTerm == null || searchTerm.isEmpty() || value.toLowerCase().contains(searchTerm.toLowerCase()));
        }
    }
}
