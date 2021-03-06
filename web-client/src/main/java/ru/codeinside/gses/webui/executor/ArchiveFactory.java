/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.webui.executor;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.vaadin.data.Item;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.ServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.form.FormPropertyImpl;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.HistoricVariableUpdateEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang.StringUtils;
import ru.codeinside.adm.AdminServiceProvider;
import ru.codeinside.adm.database.Bid;
import ru.codeinside.gses.API;
import ru.codeinside.gses.activiti.FormDecorator;
import ru.codeinside.gses.activiti.FormID;
import ru.codeinside.gses.activiti.forms.CloneTreeProvider;
import ru.codeinside.gses.activiti.forms.CustomTaskFormHandler;
import ru.codeinside.gses.activiti.forms.PropertyTree;
import ru.codeinside.gses.activiti.forms.PropertyTreeProvider;
import ru.codeinside.gses.activiti.history.VariableFormData;
import ru.codeinside.gses.activiti.history.VariableSnapshot;
import ru.codeinside.gses.lazyquerycontainer.LazyQueryContainer;
import ru.codeinside.gses.lazyquerycontainer.LazyQueryDefinition;
import ru.codeinside.gses.lazyquerycontainer.QueryDefinition;
import ru.codeinside.gses.service.F1;
import ru.codeinside.gses.service.F2;
import ru.codeinside.gses.service.Fn;
import ru.codeinside.gses.vaadin.JsonFormIntegration;
import ru.codeinside.gses.webui.ActivitiApp;
import ru.codeinside.gses.webui.Flash;
import ru.codeinside.gses.webui.components.api.IRefresh;
import ru.codeinside.gses.webui.data.OwnHistoryQueryFactory;
import ru.codeinside.gses.webui.form.EFormBuilder;
import ru.codeinside.gses.webui.form.FieldTree;
import ru.codeinside.gses.webui.form.GridForm;
import ru.codeinside.gses.webui.form.JsonForm;
import ru.codeinside.gses.webui.utils.Components;
import ru.codeinside.gses.webui.wizard.ExpandRequired;

import javax.xml.bind.DatatypeConverter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO: првести рефакторинг этой мешанины кода
final public class ArchiveFactory implements Serializable {

  private static final long serialVersionUID = -3060552897820352216L;

  final static private SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm");

  public static Component create() {
    final Table bidsTable = createBidsTable();
    final Table phaseTable = createPhaseTable();
    bidsTable.addGeneratedColumn("id", new IdColumnGenerator(bidsTable, phaseTable));

    final HorizontalLayout bidsLayout = new HorizontalLayout();
    bidsLayout.setSizeFull();
    bidsLayout.setMargin(true);
    bidsLayout.setSpacing(true);
    bidsLayout.addComponent(bidsTable);

    final VerticalLayout candidateLayout = new VerticalLayout();
    candidateLayout.setSizeFull();
    candidateLayout.setMargin(true);
    candidateLayout.addComponent(phaseTable);

    final TasksSplitter vSplitter = new TasksSplitter((LazyQueryContainer) bidsTable.getContainerDataSource(), new TableRefresh(phaseTable));
    vSplitter.setSizeFull();
    vSplitter.setFirstComponent(bidsLayout);
    vSplitter.setSecondComponent(candidateLayout);
    vSplitter.setSplitPosition(57);

    return vSplitter;
  }

  static Table createPhaseTable() {
    final Table candidate = Components.createTable("100%", "100%");

    candidate.setCaption("Этапы");
    candidate.addContainerProperty("id", String.class, null);
    candidate.addContainerProperty("name", String.class, null);
    candidate.addContainerProperty("type", String.class, null);
    candidate.addContainerProperty("date", String.class, null);
    candidate.addContainerProperty("declarant", String.class, null);
    candidate.addContainerProperty("form", Component.class, null);

    candidate.setVisibleColumns(new Object[]{"id", "name", "type", "date", "declarant", "form"});
    candidate.setColumnHeaders(new String[]{"Id", "Название", "Тип", "Даты исполнения", "Исполнитель", ""});
    candidate.setSelectable(false);
    return candidate;
  }

  static Table createBidsTable() {
    QueryDefinition queryDefinition = new LazyQueryDefinition(false, 20);
    queryDefinition.addProperty("id", String.class, null, true, true);
    queryDefinition.addProperty("procedure", String.class, null, true, true);
    queryDefinition.addProperty("startDate", String.class, null, true, true);
    queryDefinition.addProperty("finishDate", String.class, null, true, true);

    Table bidTable = new Table("Заявки", new LazyQueryContainer(queryDefinition, new OwnHistoryQueryFactory()));
    bidTable.setSizeFull();
    bidTable.setColumnHeaders(new String[]{"№ Заявки", "Процедура", "Дата заявления", "Дата завершения"});
    bidTable.setSelectable(false);
    bidTable.setSortDisabled(true);

    bidTable.setColumnExpandRatio("id", 0.1f);
    bidTable.setColumnExpandRatio("procedure", 0.5f);
    bidTable.setColumnExpandRatio("startDate", 0.2f);
    bidTable.setColumnExpandRatio("finishDate", 0.2f);

    return bidTable;
  }

  final public static class TableRefresh implements IRefresh, Serializable {
    private static final long serialVersionUID = -3060552897820352219L;
    private final Table[] tables;

    public TableRefresh(Table... tables) {
      this.tables = tables;
    }

    @Override
    public void refresh() {
      for (Table t : tables) {
        t.removeAllItems();
      }
    }
  }


  final private static class IdColumnGenerator implements Table.ColumnGenerator {
    private static final long serialVersionUID = 1L;
    private final Table bidsTable;
    private final Table phaseTable;

    public IdColumnGenerator(final Table bidsTable, final Table phaseTable) {
      this.bidsTable = bidsTable;
      this.phaseTable = phaseTable;
    }

    public Component generateCell(Table source, Object itemId, Object columnId) {
      final Item item = bidsTable.getContainerDataSource().getItem(itemId);
      final String id = (String) item.getItemProperty("id").getValue();
      return new Button(id, new IdClickListener(id, phaseTable));
    }

  }


  final static class IdClickListener implements Button.ClickListener {
    private static final long serialVersionUID = 1L;

    private final String bidId;
    private final Table phaseTable;

    public IdClickListener(String bidId, Table phaseTable) {
      this.bidId = bidId;
      this.phaseTable = phaseTable;
    }

    @Override
    public void buttonClick(ClickEvent event) {
      phaseTable.setCaption("Этапы для заявки #" + bidId);
      final Bid bid = AdminServiceProvider.get().getBid(bidId);

      phaseTable.removeAllItems();

      int index = 0;
      List<HistoricActivityInstance> histories = Fn.withEngine(new GetHistoricInstances(), bid);
      for (final ActivityImpl activity : getActivityList(bid)) {
        HistoricActivityInstance cur = activityInstance(activity, histories);
        String assignee = getAssignee(bid, activity, cur);
        Button button = null;
        if (Flash.login().equals(assignee)) {
          if (canShowUi(activity)) {
            button = new Button("просмотреть");
            Date time = cur.getEndTime() == null ? cur.getStartTime() : cur.getEndTime();
            button.addListener(new ShowClickListener(activity.getId(), bidId, time));
          }
        }
        String actName = activity.getProperty("name") != null ? activity.getProperty("name").toString() : "Без названия";
        String executionDate = getExecutionDate(cur);
        phaseTable.addItem(new Object[]{activity.getId(), actName, activity.getProperty("type").toString(), executionDate, assignee, button}, index++);
      }
    }
  }

  public final static class ShowClickListener implements Button.ClickListener {
    private final String activityId;
    private final String bidId;
    private final Date toDate;

    public ShowClickListener(String activityId, String bidId, Date toDate) {
      this.activityId = activityId;
      this.bidId = bidId;
      this.toDate = toDate;
    }

    @Override
    public void buttonClick(ClickEvent event) {
      ActivitiApp app = (ActivitiApp) event.getButton().getApplication();
      Window win = Fn.withEngine(new CreateUi(), new Object[]{bidId, activityId, toDate}, app);
      event.getButton().getWindow().addWindow(win);
      AdminServiceProvider.get().createLog(Flash.getActor(), "Activity", activityId, "View in archive", "", true);
    }
  }

  static List<ActivityImpl> getActivityList(final Bid bid) {
    return Fn.withEngine(new GetActivities(), Flash.login(), bid);
  }

  static boolean canShowUi(final ActivityImpl activity) {
    return
      isPropertyType(activity, "startEvent") ||
        (isPropertyType(activity, "userTask") && activity.getActivityBehavior() instanceof UserTaskActivityBehavior);
  }


  static String getExecutionDate(HistoricActivityInstance cur) {
    String executionDate;
    if (cur != null) {
      executionDate = formatter.format(cur.getStartTime());
      if (cur.getEndTime() != null) {
        executionDate += " - " + formatter.format(cur.getEndTime());
      }
    } else {
      executionDate = "";
    }
    return executionDate;
  }

  static Map<String, String> getHistoryValues(Bid bid, Date toDate) {
    Map<String, String> values = Maps.newHashMap();
    for (HistoricVariableUpdateEntity hu : getLastVariableValues(bid, toDate)) {
      String value = null;
      Object rawValue = hu.getValue();
      if (rawValue == null) {
        value = "";
      } else if (hu.getVariableType().getTypeName().equals("date")) {
        value = new SimpleDateFormat("dd/MM/yyyy").format(rawValue);
      } else {
        if (rawValue instanceof byte[]) {
          byte[] bytes = (byte[]) rawValue;
          try {
            value = new String(bytes, "UTF-8");
          } catch (UnsupportedEncodingException e) {
            value = toBase64HumanString(bytes);
          }
        }
        if (value == null) {
          value = rawValue.toString();
        }
      }
      values.put(hu.getName(), value);
    }
    return values;
  }

  private static Collection<HistoricVariableUpdateEntity> getLastVariableValues(Bid bid, Date toDate) {
    Map<String, HistoricVariableUpdateEntity> lastChanges = Maps.newHashMap();
    {
      List<HistoricDetail> details = Fn.withEngine(new GetHistoricVariableUpdatesOrderByTimeDesc(), bid);
      for (HistoricDetail detail : details) {
        HistoricVariableUpdateEntity update = (HistoricVariableUpdateEntity) detail;
        if (update.getTime().compareTo(toDate) <= 0) {
          String variableName = update.getVariableName();
          if (!lastChanges.containsKey(variableName)) {
            lastChanges.put(variableName, update);
          }
        }
      }
    }
    return lastChanges.values();
  }

  public static String toBase64HumanString(byte[] bytes) {
    return Joiner.on('\n').join(
      Splitter.fixedLength(64).split(DatatypeConverter.printBase64Binary(bytes))
    );
  }

  static HistoricActivityInstance activityInstance(final ActivityImpl ac, List<HistoricActivityInstance> histories) {
    HistoricActivityInstance result = null;
    for (HistoricActivityInstance hi : histories) {
      if (ac.getId().equals(hi.getActivityId())) {
        result = hi;
        break;
      }
    }
    return result;
  }

  static boolean isPropertyType(ActivityImpl ac, String typeName) {
    return typeName.equals(ac.getProperty("type").toString());
  }

  static String getAssignee(Bid bid, ActivityImpl ac, HistoricActivityInstance cur) {
    String assignee = "";
    if (isPropertyType(ac, "startEvent")) {
      assignee = bid.getDeclarant();
    } else if (cur != null) {
      assignee = cur.getAssignee();
    }
    return assignee;
  }

  static Window createFormWindow(Component su, ActivityImpl ac, String id) {
    VerticalLayout content = new VerticalLayout();
    content.setMargin(true);
    content.addComponent(su);
    Window window = new Window();
    window.setWidth(800 + 50, Sizeable.UNITS_PIXELS);
    window.setHeight(600 + 100, Sizeable.UNITS_PIXELS);
    window.center();
    window.setContent(content);
    window.setCaption("Форма этапа " + ac.getId() + " по заявке #" + id);
    if (su instanceof JsonFormIntegration || su instanceof ExpandRequired) {
      content.setSizeFull();
      content.setExpandRatio(su, 1f);
    } else {
      window.setResizable(false); // нет подстройки под размер
    }
    return window;
  }


  // ----------------- persistence -----------------

  final private static class GetHistoricInstances implements F1<List<HistoricActivityInstance>, Bid> {
    public List<HistoricActivityInstance> apply(final ProcessEngine engine, final Bid bid) {
      return engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(bid.getProcessInstanceId()).list();
    }
  }

  final private static class GetActivities implements F2<List<ActivityImpl>, String, Bid> {
    @Override
    public List<ActivityImpl> apply(ProcessEngine engine, String login, Bid bid) {
      RepositoryServiceImpl impl = (RepositoryServiceImpl) engine.getRepositoryService();
      String processDefinitionId = bid.getProcedureProcessDefinition().getProcessDefinitionId();
      engine.getIdentityService().setAuthenticatedUserId(login);
      ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) impl.getDeployedProcessDefinition(processDefinitionId);
      return processDefinition.getActivities();
    }
  }

  final static class CreateUi implements F2<Window, Object[], ActivitiApp> {
    @Override
    public Window apply(ProcessEngine engine, Object[] arg, ActivitiApp app) {
      String bidId = (String) arg[0];
      String activityId = (String) arg[1];
      Date toDate = (Date) arg[2];
      Bid bid = AdminServiceProvider.get().getBid(bidId);
      for (ActivityImpl activity : Fn.withEngine(new GetActivities(), Flash.login(), bid)) {
        if (activityId.equals(activity.getId())) {
          Component form = createGridForm(bid, activity, engine, app, toDate);
          return createFormWindow(form, activity, bidId);
        }
      }
      throw new IllegalStateException();
    }
  }

  static Component createGridForm(Bid bid, ActivityImpl activity, ProcessEngine engine, ActivitiApp app, Date toDate) {

    //TODO: в контексте Activity через commandExecutor и defaultExpression.getValue(new VariableScope())

    Map<String, String> historyValues = getHistoryValues(bid, toDate);
    FormDecorator decorator;
    if (isPropertyType(activity, "startEvent")) {
      String processDefinitionId = bid.getProcedureProcessDefinition().getProcessDefinitionId();
      CommandExecutor commandExecutor = ((ServiceImpl) engine.getRuntimeService()).getCommandExecutor();
      FormData formData = commandExecutor.execute(new GetStartArchiveFormCmd(processDefinitionId, historyValues));
      PropertyTree nodeMap = ((PropertyTreeProvider) formData).getPropertyTree();
      Map<String, VariableSnapshot> map = ImmutableMap.of();

      decorator = new FormDecorator(FormID.byProcessDefinitionId(processDefinitionId), new VariableFormData(formData, map, nodeMap));
    } else if (isPropertyType(activity, "userTask") && activity.getActivityBehavior() instanceof UserTaskActivityBehavior) {
      CustomTaskFormHandler taskFormHandler = (CustomTaskFormHandler) ((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDefinition().getTaskFormHandler();
      TaskFormData taskFormData = taskFormHandler.createTaskForm(historyValues);
      Map<String, VariableSnapshot> map = ImmutableMap.of();
      PropertyTree nodeMap = ((PropertyTreeProvider) taskFormData).getPropertyTree();

      decorator = new FormDecorator(null, new VariableFormData(taskFormData, map, nodeMap));
    } else {
      decorator = null;
    }
    if (decorator == null) {
      return null;
    }

    // простые значения по умолчанию
    CloneTreeProvider ctp = (CloneTreeProvider) decorator.variableFormData.formData;
    for (FormPropertyHandler handler : ctp.getCloneTree().handlers) {
      String id = handler.getId();
      for (FormProperty property : ctp.getCloneTree().properties) {
        if (id.equals(property.getId())) {
          if (!property.isWritable() && property.getValue() == null) {
            boolean noVar = handler.getVariableName() == null;
            boolean noVarExpression = handler.getVariableExpression() == null;
            Expression defaultExpression = handler.getDefaultExpression();
            if (defaultExpression != null && noVar && noVarExpression) {
              String text = defaultExpression.getExpressionText();
              if (!text.contains("${") && !text.contains("#{")) { // нет реальной поддержки expression!
                ((FormPropertyImpl) property).setValue(text);
              }
            }
          }
          break;
        }
      }
    }

    if (StringUtils.isNotEmpty(decorator.variableFormData.formData.getFormKey())) {
      return new EFormBuilder(decorator, true).getForm(null, null);
    }

    Map<String, FormProperty> generalProperties = decorator.getGeneral();
    if (generalProperties.containsKey(API.JSON_FORM)) {
      String templateRef = generalProperties.get(API.JSON_FORM).getValue();
      if (templateRef != null) {
        Set<String> keys = new HashSet<String>(generalProperties.keySet());
        keys.remove(API.JSON_FORM);
        for (String key : keys) {
          FormProperty property = generalProperties.get(key);
          if (property.getType() != null && "signature".equals(property.getType().getName())) {
            continue;
          }
          return JsonForm.createIntegration(decorator.id, app, templateRef, property.getValue(), true);
        }
      }
    }

    FieldTree fieldTree = new FieldTree();
    fieldTree.create(decorator);
    GridForm form = new GridForm(decorator.toSimple(), fieldTree);
    form.setImmediate(true);
    return form;
  }

  final private static class GetHistoricVariableUpdatesOrderByTimeDesc implements F1<List<HistoricDetail>, Bid> {
    @Override
    public List<HistoricDetail> apply(ProcessEngine engine, Bid bid) {
      String processInstanceId = bid.getProcessInstanceId().trim();
      return engine
        .getHistoryService()
        .createHistoricDetailQuery()
        .processInstanceId(processInstanceId)
        .variableUpdates()
        .orderByTime()
        .desc()
        .list();
    }
  }

}


