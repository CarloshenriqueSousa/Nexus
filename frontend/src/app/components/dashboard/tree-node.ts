import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-tree-node',
  imports: [CommonModule],
  templateUrl: './tree-node.html',
  styleUrl: './tree-node.css'
})
export class TreeNodeComponent {
  readonly node = input.required<any>();
  readonly folderSelected = output<any>();
  readonly deleteRequested = output<any>();
  readonly renameRequested = output<any>();
  readonly moveRequested = output<any>();

  protected readonly expanded = signal(false);

  toggle(): void {
    if (this.node().eh_pasta) {
      this.expanded.update(v => !v);
    }
  }

  onSelectFolder(node: any, event: Event): void {
    event.stopPropagation();
    this.folderSelected.emit(node);
  }

  onRenameNode(node: any, event: Event): void {
    event.stopPropagation();
    const tipo = node.eh_pasta ? 'pasta' : 'arquivo';
    const newName = prompt(`Novo nome para o ${tipo}:`, node.nome);
    if (newName && newName.trim()) {
      this.renameRequested.emit({ node, newName: newName.trim() });
    }
  }

  onMoveNode(node: any, event: Event): void {
    event.stopPropagation();
    this.moveRequested.emit(node);
  }

  onDeleteNode(node: any, event: Event): void {
    event.stopPropagation();
    const msg = node.eh_pasta 
      ? `Deseja realmente excluir a pasta '${node.nome}'? Todo o seu conteúdo (subpastas e arquivos) será permanentemente excluído.` 
      : `Deseja realmente excluir o arquivo '${node.nome}'?`;
    if (confirm(msg)) {
      this.deleteRequested.emit(node);
    }
  }
}
