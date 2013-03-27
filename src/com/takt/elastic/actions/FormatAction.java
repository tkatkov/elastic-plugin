/*
 * Copyright 2013 Taras Katkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.takt.elastic.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class FormatAction extends AnAction {

    private static String trimEnd(String str)
    {
        int len = str.length();
        char[] val = str.toCharArray();

        while ((0 < len) && (val[len - 1] <= ' '))
        {
            len--;
        }

        return (len < str.length()) ? str.substring(0, len) : str;
    }

    private static String extractLine(Document doc, int lineNumber)
    {
        int lineSeparatorLength = doc.getLineSeparatorLength(lineNumber);
        int startOffset = doc.getLineStartOffset(lineNumber);
        int endOffset = doc.getLineEndOffset(lineNumber) + lineSeparatorLength;

        return doc.getCharsSequence().subSequence(startOffset, endOffset).toString();
    }

    /**
     * Disable when no project open or no selection
     *
     * @param	event	Action system event
     */
    public void update( AnActionEvent event ) {
        boolean  enabled = false;

        final Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if (project != null && editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            boolean hasSelection = selectionModel.hasSelection();
            if (hasSelection) {
                final Document document = editor.getDocument();

                int lineNumberSelStart = document.getLineNumber(selectionModel.getSelectionStart());
                int lineNumberSelEnd   = document.getLineNumber(selectionModel.getSelectionEnd());

                if (lineNumberSelEnd > lineNumberSelStart) {
                    enabled	= true;
                }
            }
        }

        event.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(final AnActionEvent event)
    {
        final Project currentProject = event.getData(PlatformDataKeys.PROJECT);

        CommandProcessor.getInstance().executeCommand(currentProject, new Runnable()
        {
            public void run()
            {
                ApplicationManager.getApplication().runWriteAction(new Runnable()
                {
                    public void run()
                    {
                        Editor editor = event.getData(PlatformDataKeys.EDITOR);

                        if (editor == null) return;

                        SelectionModel selectionModel = editor.getSelectionModel();

                        if (! selectionModel.hasSelection()) return;

                        int offsetStart	= selectionModel.getSelectionStart();
                        int offsetEnd	= selectionModel.getSelectionEnd();

                        final Document document = editor.getDocument();

                        int lineNumberSelStart = document.getLineNumber(offsetStart);
                        int lineNumberSelEnd   = document.getLineNumber(offsetEnd  );

                        if( lineNumberSelEnd > lineNumberSelStart )
                        {
                            int amountLines = lineNumberSelEnd - lineNumberSelStart;
                            List<String> linesList = new ArrayList<String>( amountLines );

                            for (int i = lineNumberSelStart; i <= lineNumberSelEnd; i++)
                            {
                                linesList.add( extractLine(document, i) );
                            }

                            String sep = "=";

                            if ( ! linesList.get(0).contains( sep ) ) sep = ":";
                            if ( ! linesList.get(0).contains( sep ) ) sep = "as";
                            if ( ! linesList.get(0).contains( sep ) ) sep = "import";
                            if ( ! linesList.get(0).contains( sep ) ) sep = ",";
                            if ( ! linesList.get(0).contains( sep ) ) return;

                            if ( sep.equals( "," ) )
                            {
                                int[] maxB = new int[42];
                                for(int i = 0; i < 42; i++) maxB[i] = 0;

                                for (String cs : linesList)
                                {
                                    if ( ! cs.contains( sep ) ) continue;

                                    String[] parts = cs.split( sep );

                                    String part1 = trimEnd(parts[0]);
                                    if ( part1.length() > maxB[0] ) maxB[0] = part1.length();

                                    for(int i = 1; i < parts.length; i++)
                                    {
                                        String part = parts[i].trim();
                                        if ( part.length() > maxB[i] ) maxB[i] = part.length();
                                    }
                                }
                                for(int i = 0; i < amountLines; i++)
                                {
                                    String cs = linesList.get(i);

                                    if ( ! cs.contains( sep ) ) continue;

                                    String[] parts = cs.split( sep );

                                    String startPart = trimEnd( parts[0] );

                                    int ll = maxB[0] - startPart.length();
                                    String fmt = "";
                                    if ( ll > 0 ) fmt = String.format("%-" + ll + "s", "");

                                    String nl = String.format("%s,%s", startPart, fmt);

                                    for(int j = 1; j < parts.length; j++)
                                    {
                                        String part = parts[j].trim();
                                        ll = maxB[j] - part.length();

                                        if ( j == parts.length-1 )
                                        {
                                            nl += part;
                                            continue;
                                        }

                                        fmt = "";
                                        if ( ll > 0 ) fmt = String.format("%-" + ll + "s", "");

                                        nl += String.format("%s,%s", part, fmt);
                                    }
                                    linesList.set(i, nl+'\n');
                                }
                            }
                            else
                            {
                                int maxB = 0;

                                for (String cs : linesList)
                                {
                                    if ( ! cs.contains( sep ) ) continue;

                                    String part1 = trimEnd(cs.split( sep )[0]);

                                    if ( part1.length() > maxB ) maxB = part1.length();

                                }
                                for(int i = 0; i < amountLines; i++)
                                {
                                    String cs = linesList.get(i);

                                    if ( ! cs.contains( sep ) ) continue;

                                    String[] parts = cs.split( sep );

                                    String startPart = trimEnd( parts[0] );

                                    StringBuilder sb = new StringBuilder(parts[1]);
                                    for(int j = 2; j < parts.length; j++)
                                    {
                                        sb.append( sep );
                                        sb.append( parts[j] );
                                    }

                                    String endPart = sb.toString().trim();

                                    int ll = maxB - startPart.length();
                                    String fmt = "";
                                    if ( ll > 0 ) fmt = String.format("%-" + ll + "s", "");

                                    linesList.set(i, String.format("%s%s %s %s\n", startPart, fmt, sep, endPart));
                                }
                            }

                            StringBuilder sb = new StringBuilder();
                            for (String aLinesList : linesList) sb.append(aLinesList);

                            offsetStart	= document.getLineStartOffset(lineNumberSelStart);
                            offsetEnd	= document.getLineEndOffset  (lineNumberSelEnd  );

                            String rr = sb.toString();

                            document.replaceString( offsetStart, offsetEnd, rr.substring(0,rr.length()-1) );
                        }
                    }
                });

            }}, "Format Lines with Elastic Tabs", UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
    }
}
