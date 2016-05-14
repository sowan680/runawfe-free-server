/*
 * This file is part of the RUNA WFE project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; version 2.1
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wf.web.tag;

import java.util.List;

import org.apache.ecs.html.TD;
import org.tldgen.annotations.BodyContent;

import ru.runa.af.web.MessagesExecutor;
import ru.runa.common.web.html.HeaderBuilder;
import ru.runa.common.web.html.RowBuilder;
import ru.runa.common.web.html.StringsHeaderBuilder;
import ru.runa.common.web.html.TableBuilder;
import ru.runa.wf.web.MessagesProcesses;
import ru.runa.wf.web.html.ProcessSwimlaneRowBuilder;
import ru.runa.wfe.execution.ProcessPermission;
import ru.runa.wfe.execution.dto.WfSwimlane;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.service.delegate.Delegates;

import com.google.common.collect.Lists;

@org.tldgen.annotations.Tag(bodyContent = BodyContent.EMPTY, name = "processSwimlaneMonitor")
public class ProcessSwimlaneMonitorTag extends ProcessBaseFormTag {

    private static final long serialVersionUID = -5024428545159087986L;

    @Override
    protected boolean isFormButtonVisible() {
        return false;
    }

    @Override
    protected void fillFormData(TD tdFormElement) {
        List<WfSwimlane> swimlanes = Delegates.getExecutionService().getSwimlanes(getUser(), getIdentifiableId());
        List<String> headerNames = Lists.newArrayList();
        headerNames.add(MessagesProcesses.LABEL_SWIMLANE_NAME.message(pageContext));
        headerNames.add(MessagesProcesses.LABEL_SWIMLANE_ASSIGNMENT.message(pageContext));
        headerNames.add(MessagesExecutor.LABEL_SWIMLANE_ORGFUNCTION.message(pageContext));
        HeaderBuilder headerBuilder = new StringsHeaderBuilder(headerNames);
        RowBuilder rowBuilder = new ProcessSwimlaneRowBuilder(swimlanes, pageContext);
        tdFormElement.addElement(new TableBuilder().build(headerBuilder, rowBuilder));
    }

    @Override
    protected Permission getPermission() {
        return ProcessPermission.READ;
    }

    @Override
    protected String getTitle() {
        return MessagesProcesses.TITLE_INSANCE_SWINLANE_LIST.message(pageContext);
    }
}