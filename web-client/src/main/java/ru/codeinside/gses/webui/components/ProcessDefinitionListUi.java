/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.webui.components;

import ru.codeinside.gses.webui.components.api.IRefresh;
import ru.codeinside.gses.webui.containers.LazyLoadingContainer;
import ru.codeinside.gses.webui.data.ProcessDefinitionListQuery;
import ru.codeinside.gses.webui.items.builders.ProcessDefinitionItemBuilder;
import ru.codeinside.gses.webui.utils.Components;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.data.Container;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

public class ProcessDefinitionListUi extends CustomComponent implements IRefresh {

	private static final long serialVersionUID = -8125401949388535398L;
	@AutoGenerated
	private VerticalLayout layout;
	@AutoGenerated
	private Table processDefTable;
	private ProcessDefinitionItemBuilder itemBuilder;
	private DeploymentAddUi addDeploy;

	public ProcessDefinitionListUi(ProcessDefinitionItemBuilder itemBuilder, DeploymentAddUi addDeploy) {
		this.itemBuilder = itemBuilder;
		this.addDeploy = addDeploy;
		buildMainLayout();
		setCompositionRoot(layout);
	}

	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		layout = new VerticalLayout();
		layout.setImmediate(false);
		layout.setWidth("100%");
		layout.setHeight("100%");
		layout.setStyleName(Reindeer.TABSHEET_MINIMAL);

		setWidth("100.0%");
		setHeight("100.0%");

		// table_1
		processDefTable = Components.createTable("900px", "500px");

		Container newDataSource = new LazyLoadingContainer(new ProcessDefinitionListQuery(itemBuilder), 5);

		processDefTable.setContainerDataSource(newDataSource);

		processDefTable.addContainerProperty("id", Component.class, null);
		processDefTable.addContainerProperty("name", String.class, null);
		processDefTable.addContainerProperty("key", String.class, null);
		processDefTable.addContainerProperty("run", Component.class, null);

		processDefTable.setColumnHeaders(ProcessDefinitionListQuery.COL_HEADERS_RUS);

		Panel panel = new Panel();
		panel.setContent(new VerticalLayout());

		addDeploy.setWidth("600.0px");
		addDeploy.setHeight("60.0px");

		layout.addComponent(addDeploy);
		layout.addComponent(processDefTable);

		return layout;
	}

	@Override
	public void refresh() {
		processDefTable.getContainerDataSource().removeAllItems();
		processDefTable.refreshRowCache();
	}

}
